package com.laiye.grpc2json.interceptor

import com.laiye.framework.common.util.HeaderKeys
import com.laiye.framework.common.util.SpringContextHolder
import io.grpc.*
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication

//项目A调用项目B。默认带Auth信息
open class GrpcOAuthClientAuthInterceptor : ClientInterceptor {
    val log = LoggerFactory.getLogger(ClientInterceptor::class.java);
    override fun <ReqT : Any, RespT : Any> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        options: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        val nextCall =
            object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, options)) {
                override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                    if (SecurityContextHolder.getContext() != null) {
                        val securityContext = SpringContextHolder.getBean(SecurityContext::class.java)
                        val auth = securityContext.authentication
                        if (auth is BearerTokenAuthentication) {
                            headers.put(
                                Metadata.Key.of(HeaderKeys.AUTHORIZAITON_KEY, Metadata.ASCII_STRING_MARSHALLER),
                                auth.token.tokenValue
                            )
                        }
                    } else {
                        log.warn("开启了Head安全,但是没有获取到SecurityContextHolder中的SecurityContext");
                    }
                    super.start(responseListener, headers)
                }
            }
        return nextCall;
    }
}