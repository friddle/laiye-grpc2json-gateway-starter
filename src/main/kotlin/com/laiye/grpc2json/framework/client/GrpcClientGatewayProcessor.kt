package com.laiye.grpc2json.framework.client

import com.laiye.grpc2json.config.data.GrpcChannelGatewayProperties
import io.grpc.Channel
import io.grpc.stub.AbstractBlockingStub
import io.grpc.stub.AbstractStub
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory
import net.devh.boot.grpc.client.inject.GrpcClientBeanPostProcessor
import net.devh.boot.grpc.client.nameresolver.NameResolverRegistration
import net.devh.boot.grpc.client.stubfactory.StubFactory
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext

class GrpcClientGatewayProcessor(context: ApplicationContext) : GrpcClientBeanPostProcessor(context) {
    val logger=LoggerFactory.getLogger(GrpcClientGatewayProcessor::class.java)
    val context: ApplicationContext = context
    var channelFactory: GrpcChannelFactory = getChannelFactory_(context)

    private fun getChannelFactory_(context: ApplicationContext): GrpcChannelFactory {
        if (channelFactory == null) {
            context.getBean(NameResolverRegistration::class.java)
            val factory = context.getBean(GrpcChannelFactory::class.java)
            channelFactory = factory
            return factory
        }
        return channelFactory
    }

    open fun process(
        context: ApplicationContext,
        name: String, item: GrpcChannelGatewayProperties
    ): Map<String, AbstractStub<*>> {
        val channel = channelFactory.createChannel(name, arrayListOf(), false)
        checkNotNull(channel) { "Channel factory created a null channel for $name" }
        val reflections = Reflections(item.gateway.`package`,Scanners.SubTypes)
        val stubMap = reflections.getSubTypesOf(AbstractStub::class.java).map {
            Pair(
                it.simpleName.toLowerCase()
                    .replace("service", "")
                    .replace("blockingstub", ""),
                processItem(channel, it)
            )
        }.filter { it.second!=null }.map { Pair(it.first,it.second!!) }.toMap()
        if(stubMap.isEmpty()){
            logger.warn("package:${item.gateway.`package`} has no proto .check package settings")
        }
        return stubMap
    }

    private fun processItem(
        channel: Channel, stub: Class<out AbstractStub<*>>
    ): AbstractStub<*>? {
        if(!stub.name.lowercase().contains("blocking")){
            return null;
        }
        var stubClass=if(stub is AbstractBlockingStub<*>){
            createStubByFactory(
                stub,
                channel
            ) as AbstractStub?
        }else{
            createStubByStubSelf(stub,channel) as AbstractStub<*>?
        }
        return stubClass
    }


    private  fun createStubByStubSelf(stubClass: Class<out AbstractStub<*>>,channel: Channel):AbstractStub<*>?{
        val construct=stubClass.getDeclaredConstructor(Channel::class.java);
        construct.isAccessible=true;
        val stub=construct.newInstance(channel);
        return stub as AbstractStub<*>;

    }

    private fun createStubByFactory(stubClass: Class<out AbstractStub<*>>, channel: Channel): AbstractStub<*>? {
        val factory =
            context.getBeansOfType(StubFactory::class.java).filterValues {
                it.isApplicable(stubClass)
            }.values.firstOrNull()?:return null
        return factory.createStub(stubClass, channel)
    }


}