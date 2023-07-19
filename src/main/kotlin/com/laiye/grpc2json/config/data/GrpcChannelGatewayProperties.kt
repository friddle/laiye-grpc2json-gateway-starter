package com.laiye.grpc2json.config.data

import net.devh.boot.grpc.client.config.GrpcChannelProperties

class GrpcChannelGatewayPropertiesItem {
    var enable: Boolean = true
    var prefix: String = ""
    var channelKey: String = ""
    var `package`: String? = ""
}

class GrpcChannelGatewayProperties : GrpcChannelProperties() {
    var gateway: GrpcChannelGatewayPropertiesItem = GrpcChannelGatewayPropertiesItem()
}