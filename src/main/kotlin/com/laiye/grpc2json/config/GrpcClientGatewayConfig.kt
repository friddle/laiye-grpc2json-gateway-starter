package com.laiye.grpc2json.config

import com.laiye.grpc2json.config.data.GrpcChannelGatewayProperties
import com.laiye.grpc2json.framework.client.GrpcClientServlet
import net.devh.boot.grpc.client.config.GrpcChannelsProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.servlet.Servlet

@Configuration
@ConditionalOnClass(value = [GrpcChannelsProperties::class])
@ConditionalOnExpression("#{'true'.equals(environment.getProperty('grpc.gateway.client.enable'))}")
@ConfigurationProperties("grpc")
open class GrpcClientGatewayConfig {
    val log = LoggerFactory.getLogger(GrpcServerGateWayConfig::class.java)
    var client: Map<String, GrpcChannelGatewayProperties> = hashMapOf()

    @Value("\${grpc.gateway.client.prefix:/api/gateway/client/*}")
    var prefix: String = "";

    @Value("\${grpc.gateway.client.output:rest}")
    var outputType: GrpcGatewayOutputType = GrpcGatewayOutputType.rest

}