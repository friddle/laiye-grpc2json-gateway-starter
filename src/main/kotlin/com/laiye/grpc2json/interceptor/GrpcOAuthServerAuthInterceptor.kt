package com.laiye.grpc2json.interceptor

import com.laiye.framework.common.util.HeaderKeys
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication
import org.springframework.security.oauth2.server.resource.introspection.NimbusOpaqueTokenIntrospector
import java.time.Instant


//项目A调用项目B。默认带Auth信息
open class GrpcOAuthServerAuthInterceptor(tokenIntrospector: NimbusOpaqueTokenIntrospector) : ServerInterceptor {
    val log = LoggerFactory.getLogger(GrpcOAuthServerAuthInterceptor::class.java);
    val tokenIntrospector = tokenIntrospector;

    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val authHeader: String =
            headers.get(Metadata.Key.of(HeaderKeys.AUTHORIZAITON_KEY, Metadata.ASCII_STRING_MARSHALLER)) ?: ""
        if (!(authHeader.startsWith("Bearer ") || authHeader.startsWith("bearer "))) {
            return next.startCall(call, headers)
        }
        val token = authHeader.substring(7)
        log.debug("Bearer Token Authorization header found")
        try {
            if (authenticationIsRequired()) {
                val principal: OAuth2AuthenticatedPrincipal = tokenIntrospector.introspect(token);
                val result: AbstractAuthenticationToken = convert(principal, token)
                log.debug("Authentication success: {}", result)
                SecurityContextHolder.getContext().authentication = result

            }
        } catch (e: Exception) {
            log.error("Authentication failed: {}", e.message)
            throw e
        }
        return next.startCall(call, headers)
    }


    open fun convert(principal: OAuth2AuthenticatedPrincipal, token: String): AbstractAuthenticationToken {
        val iat = principal.getAttribute<Instant>(OAuth2TokenIntrospectionClaimNames.IAT)
        val exp = principal.getAttribute<Instant>(OAuth2TokenIntrospectionClaimNames.EXP)
        val accessToken = OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, token, iat, exp)
        return BearerTokenAuthentication(principal, accessToken, principal.authorities)
    }

    open fun authenticationIsRequired(): Boolean {
        val existingAuth: Authentication = SecurityContextHolder.getContext().authentication
        if (existingAuth != null || !existingAuth.isAuthenticated) {
            return true
        }
        return existingAuth is AnonymousAuthenticationToken
    }


}