package com.laiye.grpc2json.framework

import com.google.gson.Gson
import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import com.laiye.grpc2json.framework.server.toGsonString
import com.laiye.grpc2json.interceptor.IGrpcGateWayJsonInterceptor
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.util.regex.Pattern

val log = LoggerFactory.getLogger("grpc_gateway_framework")
val gson = Gson();

fun convertToMessage(
    requestType: Class<out GeneratedMessageV3>,
    body: String,
    interceptors: List<IGrpcGateWayJsonInterceptor> = arrayListOf()
): Triple<Boolean, String, Message?> {
    var newBody = interceptors.fold(body) { bodyItem, interceptor -> interceptor.interceptorReq(bodyItem, requestType) }
    val parser = JsonFormat.parser().ignoringUnknownFields()
    try {
        var builder = requestType.getMethod("newBuilder").invoke(null) as GeneratedMessageV3.Builder<*>
        val requestBody = if (StringUtils.isBlank(body)) {
            "{}"
        } else {
            newBody
        }
        parser.merge(requestBody, builder)
        return Triple(true, "", builder.build())
    } catch (e: Throwable) {
        log.error("json转换gRPC对象出错", e);
        return Triple(
            false, "无法转换Json到对象\r:" + requestType.simpleName + "\r:\r" + body +
                    "\r:请注意请求是否正确", null
        );
    }
}

fun getClassName(name: String): String {
    val r = Pattern.compile("\\$((\\w+))")
    val m = r.matcher(name)
    if (m.find()) {
        return m.group(0).replace("$", "")
    }
    return name;
}

fun outputRsp(
    response: HttpServletResponse,
    message: GeneratedMessageV3,
    headers: Map<String, String> = hashMapOf(),
    interceptors: List<IGrpcGateWayJsonInterceptor> = arrayListOf()
) {
    val printer = JsonFormat.printer().includingDefaultValueFields()
    var result = printer.print(message)
    result = interceptors.fold(result) { result, interceptor -> interceptor.interceptorRsp(result, message) }
    response.contentType = "application/json;charset=UTF-8"
    response.characterEncoding = "UTF-8"
    headers.forEach {
        response.addHeader(it.key, it.value)
    }
    val writer = response.writer
    writer.println(result)
}


fun quickToErrorMsg(
    req: HttpServletRequest, resp: HttpServletResponse, e: Throwable,
    jsonBody: String
) {
    log.error("错误调用:${req.pathInfo}\r${jsonBody}", e);
    if (e is IllegalArgumentException) {
        writeError(Result.failure<Any>(e), resp)
        return
    }
    if(e.cause!=null&&e.cause is IllegalArgumentException){
        val cause=e.cause as IllegalArgumentException;
        writeError(Result.failure<Any>(cause), resp)
        return
    }
    throw e;
}


fun writeError(info: Any, resp: HttpServletResponse) {
    IOUtils.write(info.toGsonString(), resp.outputStream, Charset.forName("UTF-8"))
    resp.contentType = "application/json;charset=UTF-8"
    resp.characterEncoding = "UTF-8"
    resp.flushBuffer()
}