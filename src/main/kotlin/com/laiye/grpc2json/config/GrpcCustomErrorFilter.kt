package com.laiye.grpc2json.config

import cn.hutool.core.io.IoUtil
import com.fasterxml.jackson.databind.ext.SqlBlobSerializer
import com.google.gson.Gson
import com.laiye.framework.common.exception.BusinessException
import com.laiye.framework.common.exception.DefaultExceptionAdvice
import io.grpc.StatusRuntimeException
import org.apache.commons.io.IOUtils
import org.springframework.web.HttpMediaTypeException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.filter.GenericFilterBean
import java.lang.reflect.InvocationTargetException
import java.nio.charset.Charset
import java.sql.SQLException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletResponse

class GrpcCustomErrorFilter(advice: DefaultExceptionAdvice):GenericFilterBean(){
    val gson= Gson();
    val advice=advice

    override fun doFilter(p0: ServletRequest, p1: ServletResponse, p2: FilterChain) {
        try{
            p2.doFilter(p0,p1)
        }catch (e:Exception){
            return dealWithException(e,p1);
        }
    }

    fun dealWithException(e:Exception,rsp:ServletResponse){
        var exception:Throwable=e;
        if(e !is StatusRuntimeException&&e.cause!=null){
            exception=e.cause!!
        }
        if(e is ServletException){
            exception=e.rootCause
        }
        if(e is InvocationTargetException&&e.targetException!=null){
            exception=e.targetException;
        }
        val result=when{
            exception is StatusRuntimeException -> advice.handleException(exception)
            exception is SQLException -> advice.handleException(exception)
            exception is HttpMediaTypeNotSupportedException->advice.handleHttpMediaTypeNotSupportedException(exception);
            else->advice.handleException(exception);
        }
        IOUtils.write(gson.toJson(result),rsp.outputStream, Charset.defaultCharset());
        return
    }
}