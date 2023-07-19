package com.laiye.grpc2json.framework.server

import com.google.gson.Gson
import com.google.protobuf.GeneratedMessageV3
import com.laiye.framework.common.exception.BaseCode
import com.laiye.framework.common.exception.BusinessException
import com.laiye.framework.common.exception.SystemCodeEnum
import com.laiye.grpc2json.config.GrpcGateWayCommonConfig
import com.laiye.grpc2json.framework.convertToMessage
import com.laiye.grpc2json.framework.quickToErrorMsg
import com.laiye.grpc2json.interceptor.IGrpcGateWayJsonInterceptor
import com.laiye.grpc2json.interceptor.annotations.GrpcServerGateWayJsonInterceptor
import io.grpc.BindableService
import net.devh.boot.grpc.server.service.GrpcService
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

var gson = Gson()
inline fun Any.toGsonString(): String {
    return gson.toJson(this)
}

open class GrpcDispatchServlet(val context: ApplicationContext, val config: GrpcGateWayCommonConfig) : HttpServlet() {
    var services: MutableMap<String, BindableService> = hashMapOf()
    var gson = Gson()
    var log = LoggerFactory.getLogger(GrpcDispatchServlet::class.java)
    var jsonInterceptors: List<IGrpcGateWayJsonInterceptor> = arrayListOf();

    init {
        getServices(context)
        getJsonInterceptors(context)
    }

    fun getJsonInterceptors(context: ApplicationContext): List<IGrpcGateWayJsonInterceptor> {
        if(jsonInterceptors.isEmpty()){
            jsonInterceptors = context.getBeansWithAnnotation(GrpcServerGateWayJsonInterceptor::class.java).values.map { it as IGrpcGateWayJsonInterceptor }
            log.info("registry grpc gateway server interceptors:" + jsonInterceptors.joinToString(",") { it.javaClass.simpleName })
        }
        return jsonInterceptors;
    }

    open fun getServices(context: ApplicationContext): MutableMap<String, BindableService> {
        if (services != null && services.isNotEmpty()) {
            return services!!;
        }
        val subTypes: Map<String, Any> = context.getBeansWithAnnotation(GrpcService::class.java);
        val grpcServices = subTypes.mapKeys { item ->
            item.value.javaClass.superclass.simpleName.replace("ImplBase", "")
                .replace("Service", "")
                .toLowerCase()
        }.filter { it.value is BindableService }.mapValues { it.value as BindableService }.toMutableMap()
        this.services = grpcServices
        log.info("find grpc services:" + grpcServices.keys.joinToString(","))
        return grpcServices
    }


    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
        throw BusinessException(SystemCodeEnum.GRPC_GATEWAY_METHOD_NOT_SUPPORT);
    }


    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        var requestJsonBody = ""
        val projectEnv = this.context.getBean(Environment::class.java).activeProfiles.firstOrNull() ?: "prod"
        try {
            val urls = req.pathInfo.replaceFirst("/", "").split("/")
            if (urls.size <= 1) {
                throw BusinessException(SystemCodeEnum.GRPC_GATEWAY_REQUEST_URL_ERROR)
            }
            var serviceName = urls[0].toLowerCase()
            var methodName = urls[1].toLowerCase()
            //一定要放这里。req只能执行一次。否则报错
            requestJsonBody = IOUtils.toString(req.inputStream, "UTF-8");
            this.invokeGrpc(serviceName, methodName, requestJsonBody, req, resp)
        } catch (e: Exception) {
            quickToErrorMsg(req, resp, e, requestJsonBody);
        }
    }

    @Throws(Exception::class)
    fun invokeGrpc(
        serviceName: String,
        methodName: String,
        requestJson: String,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse
    ) {
        try {
            if (!getServices(context).keys.contains(serviceName)) {
                throw IllegalArgumentException("请求类不存在" + serviceName + ":" + gson.toJson(this.getServices(context).keys))
            }
            val service = getServices(context)[serviceName]
            val method = service!!::class.java!!.declaredMethods!!.filter { it ->
                it.name.equals(methodName, ignoreCase = true)
            }.getOrNull(0)
                ?: throw BusinessException(SystemCodeEnum.GRPC_GATEWAY_REQUEST_SERVICE_NOT_FOUND)


            val requestType =
                method.parameterTypes[0] as Class<out GeneratedMessageV3> ?: GeneratedMessageV3::class.java
            val request = convertToMessage(requestType, requestJson, getJsonInterceptors(context))
            if (!request.first) {
                throw BusinessException(object :BaseCode{
                    override fun getCode(): Int {
                        return SystemCodeEnum.GRPC_GATEWAY_REQUEST_BODY_ERROR.code
                    }
                    override fun getMessage(): String {
                       return SystemCodeEnum.GRPC_GATEWAY_REQUEST_BODY_ERROR.message
                    }

                    override fun getDebugMsg(): String {
                        return request.second;
                    }
                })
            }
            method.invoke(service, request.third, GrpcMockObserver(httpRequest, httpResponse, config,getJsonInterceptors(context)))
        } catch (e: Exception) {
            throw e
        }
    }


}