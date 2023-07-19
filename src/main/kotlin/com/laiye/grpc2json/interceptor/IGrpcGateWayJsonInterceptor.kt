package com.laiye.grpc2json.interceptor

import com.google.protobuf.GeneratedMessageV3

interface IGrpcGateWayJsonInterceptor {
    fun interceptorReq(jsonBody: String, Type: Class<out GeneratedMessageV3>): String = jsonBody
    fun interceptorRsp(jsonBody: String, message: GeneratedMessageV3): String = jsonBody
}