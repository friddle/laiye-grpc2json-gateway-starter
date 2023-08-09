package com.laiye.grpc2json.config

import com.laiye.grpc2json.framework.server.GrpcDispatchServlet
import jakarta.servlet.Servlet
import org.apache.logging.slf4j.SLF4JLogger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(SLF4JLogger::class)
@ConditionalOnProperty(prefix = "grpc.gateway.server", name = ["enable"], havingValue = "true")
open class GrpcServerGateWayConfig {

    @Value("\${grpc.gateway.server.prefix:/api/gateway/server/*}")
    var prefix: String? = null;

    val log = LoggerFactory.getLogger(GrpcServerGateWayConfig::class.java)

    var output: GrpcGatewayOutputType = GrpcGatewayOutputType.rest

    @Bean
    open fun grpcGatewayRegister(@Autowired context: ApplicationContext): ServletRegistrationBean<*>? {
        val registrationBean: ServletRegistrationBean<Servlet> = ServletRegistrationBean<Servlet>(
            GrpcDispatchServlet(
                context,
                GrpcGateWayCommonConfig(outputType = output)
            )
        )
        log.info("register ${prefix} to grpc server gateway")
        registrationBean.addUrlMappings(prefix)
        return registrationBean
    }

}