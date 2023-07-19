package com.laiye.grpc2json.interceptor

import com.google.gson.Gson
import com.laiye.framework.common.util.HeaderKeys
import com.laiye.framework.common.util.HeaderKeys.AUTH_MEMBER_KEY
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder


//在header头中传SpringSecurity.Authentication对象...
open class GrpcHeadServerAuthInterceptor : ServerInterceptor {
    val gson = Gson();

    open fun setAuth(metadata: Metadata) {
        if (metadata.keys().contains(AUTH_MEMBER_KEY)) {
            val context = SecurityContextHolder.getContext();
            val key = Metadata.Key.of(HeaderKeys.AUTH_MEMBER_KEY, Metadata.BINARY_BYTE_MARSHALLER)
            val data = String(metadata.get(key) as ByteArray);
            context.authentication = gson.fromJson(data, Authentication::class.java);
        }
    }

    override fun <ReqT : Any, RespT : Any> interceptCall(
        method: ServerCall<ReqT, RespT>,
        metadata: Metadata,
        handler: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        setAuth(metadata)
        return handler.startCall(method, metadata);
    }


}