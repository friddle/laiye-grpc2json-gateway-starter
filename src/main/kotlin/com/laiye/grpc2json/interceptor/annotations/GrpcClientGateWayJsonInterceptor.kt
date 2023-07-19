package com.laiye.grpc2json.interceptor.annotations

import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.lang.annotation.*
import java.lang.annotation.Retention
import java.lang.annotation.Target

@Target(ElementType.TYPE, ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@Bean
annotation class GrpcClientGateWayJsonInterceptor