package com.laiye.grpc2json.framework.server

import com.google.protobuf.GeneratedMessageV3
import com.laiye.grpc2json.config.GrpcGateWayCommonConfig
import com.laiye.grpc2json.framework.outputRsp
import com.laiye.grpc2json.interceptor.IGrpcGateWayJsonInterceptor
import io.grpc.stub.StreamObserver
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import java.io.IOException


class GrpcMockObserver(
    val request: HttpServletRequest,
    val response: HttpServletResponse,
    val config: GrpcGateWayCommonConfig,
    val interceptor: List<IGrpcGateWayJsonInterceptor> = arrayListOf()
) : StreamObserver<Any?> {
    var values: GeneratedMessageV3? = null
    var logger = LoggerFactory.getLogger(GrpcMockObserver::class.java)

    override fun onNext(value: Any?) {
        values = value as GeneratedMessageV3?
    }

    override fun onError(t: Throwable) {}
    override fun onCompleted() {
        try {
            if (values == null) {
                throw IllegalStateException("output values is empty")
            }
            outputRsp(response, values!!, hashMapOf(), interceptor)
        } catch (e: IOException) {
            throw IllegalArgumentException("error invoke:${values?.toGsonString() ?: "null"}")
        }
    }

}