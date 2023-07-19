package com.laiye.grpc2json.framework.client

import com.google.protobuf.GeneratedMessageV3
import com.laiye.framework.common.exception.BusinessException
import com.laiye.framework.common.exception.SystemCodeEnum
import com.laiye.grpc2json.config.GrpcGateWayCommonConfig
import com.laiye.grpc2json.config.data.GrpcChannelGatewayProperties
import com.laiye.grpc2json.framework.convertToMessage
import com.laiye.grpc2json.framework.outputRsp
import com.laiye.grpc2json.framework.quickToErrorMsg
import com.laiye.grpc2json.interceptor.IGrpcGateWayJsonInterceptor
import com.laiye.grpc2json.interceptor.annotations.GrpcClientGateWayJsonInterceptor
import com.laiye.grpc2json.interceptor.annotations.GrpcServerGateWayJsonInterceptor
import io.grpc.stub.AbstractBlockingStub
import io.grpc.stub.AbstractStub
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import java.lang.reflect.Method
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

open class GrpcClientServlet(
    context: ApplicationContext,
    properties: Map<String, GrpcChannelGatewayProperties>,
    config: GrpcGateWayCommonConfig
) :
    HttpServlet() {
    val properties: Map<String, GrpcChannelGatewayProperties> = properties
    val context: ApplicationContext = context
    val config: GrpcGateWayCommonConfig = config
    var clients: Map<String, Map<String, AbstractStub<*>>> = hashMapOf()
    val log = LoggerFactory.getLogger(GrpcClientServlet::class.java)
    val beanProcessor = GrpcClientGatewayProcessor(context)
    var jsonInterceptors: List<IGrpcGateWayJsonInterceptor> = arrayListOf();

    init {
        getJsonInterceptors(context)
        getGrpcClients()
    }

    fun getJsonInterceptors(context: ApplicationContext): List<IGrpcGateWayJsonInterceptor> {
        if(jsonInterceptors.isEmpty()){
            jsonInterceptors = context.getBeansWithAnnotation(GrpcClientGateWayJsonInterceptor::class.java).values.map { it as IGrpcGateWayJsonInterceptor }
            log.info("registry grpc gateway client interceptors:" + jsonInterceptors.joinToString(",") { it.javaClass.simpleName })
        }
        return jsonInterceptors;
    }

    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
        resp!!.contentType = "text/html";
        val writer = resp.writer;
        writer.println("get method not supported");
        writer.flush();
    }

    private fun getGrpcClients(): Map<String, Map<String, AbstractStub<*>>> {
        if (this.clients.values.flatMap { it.values }.isEmpty()) {
            clients = this.properties
                .filter { it.value.gateway.enable }
                .filter { it.value.gateway.`package`?.isNotEmpty() ?: false }
                .mapValues { it.value.gateway.channelKey = it.key;it.value }
                .mapKeys {
                    it.value.gateway.prefix.ifEmpty { it.key }
                }.mapValues {
                    beanProcessor.process(context, it.value.gateway.channelKey, it.value)
                }.toMap()
            log.info("grpc2gateay registry client:"+clients.map { it.key }.joinToString(",") )
            log.info("grpc2gateay registry client:"+clients.flatMap { it.value.keys }.joinToString(",") )
            return clients;
        }
        return clients;
    }

    override fun doPost(req: HttpServletRequest, rsp: HttpServletResponse) {
        var requestJsonBody = ""
        val projectEnv = this.context.getBean(Environment::class.java).activeProfiles.firstOrNull() ?: "prod"
        try {
            val urls = req.pathInfo.replaceFirst("/", "").split("/")
            if (urls.size < 3) {
                throw BusinessException(SystemCodeEnum.GRPC_GATEWAY_REQUEST_BODY_ERROR)
            }
            var groupName = urls[0].toLowerCase()
            var serviceName = urls[1].toLowerCase()
            var methodName = urls[2].toLowerCase()
            requestJsonBody = IOUtils.toString(req.inputStream, "UTF-8");
            this.invokeGrpc(groupName, serviceName, methodName, requestJsonBody, req, rsp)
        } catch (e: Exception) {
            quickToErrorMsg(req, rsp, e, requestJsonBody)
        }
    }


    fun invokeGrpc(
        groupName: String,
        serviceName: String,
        methodName: String,
        requestJson: String,
        req: HttpServletRequest,
        rsp: HttpServletResponse
    ) {
        val client =
            getGrpcClients().get(groupName)?.get(serviceName) ?: throw BusinessException(SystemCodeEnum.GRPC_GATEWAY_REQUEST_BODY_ERROR)
        val method: Method = client.javaClass?.declaredMethods?.filter {
            it.name.equals(methodName, ignoreCase = true)
        }?.firstOrNull() ?: throw IllegalArgumentException("api not found:${groupName} ${serviceName}/${methodName}")
        val requestType = method.parameterTypes[0] as Class<out GeneratedMessageV3> ?: GeneratedMessageV3::class.java
        val request = convertToMessage(requestType, requestJson)
        if (!request.first) {
            throw BusinessException(SystemCodeEnum.GRPC_GATEWAY_REQUEST_BODY_ERROR)
        }
        val invokeRsp = method.invoke(client, request.third) as GeneratedMessageV3
        return writeRsp(rsp, invokeRsp)
    }

    fun writeRsp(rsp: HttpServletResponse, invokeRsp: GeneratedMessageV3) {
        outputRsp(rsp, invokeRsp, hashMapOf())
    }


}