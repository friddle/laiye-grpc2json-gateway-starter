Grpc2JsonGateWay使用指南
-----------------

## 目标
基本上是让Grpc的Service提供相应的http和post的调用。减少大部分的类型转换和重复调用

## 使用
引入相应的maven依赖。比如
```xml
<dependency>
    <groupId>com.laiye.framework</groupId>
    <artifactId>laiye-grpc2json-gateway-starter</artifactId>
    <version>1.1.0-SNAPSHOT</version>
</dependency>
```
### GrpcServerGateway
此项目是用来配置本地GrpcServer层的Json转Grpc请求
比如你提供了proto文件为
```proto
   message WordReq{ string hello=1; }
   message WordRsp{ string word=1; }
   service HelloService{
       rpc Word(WordReq)returns(WordRsp)
   }
```
并在代码中继承了Grpc的服务
```java
         @GrpcService
         public class GHelloService extends HelloServiceGrpc.HelloServiceImplBase{
               @Override
               public void getFileInfo(HelloReq request, StreamObserver<HelloRsp> response) {
                   HelloRsp rsp=new HelloRsp();
                   rsp.setWord(request.getHello()+" world");
                   response.onNext(rsp);
                   response.finish();
                   return;
               }
         }
```

那你可以通过
```shell
     curl -H "Content-Type: application/json" -X POST \
         -d '{ "hello":"hello" }' "http://localhost/api/gateway/hello/word" 
     结果为
     {"word":"hello world"}     
```
就可以直接调用相应的GHelloService相应的方法获得相应的返回Json。

### 引入配置
```yaml
grpc:
  gateway:
    server:
      prefix: /api/gateway/server/*
      enable: true
```
其中prefix是url前缀。enable是是否开启

###  原理
1. 扫描任何继承了GrpcService注解的Grpc继承类
2. 通过固定规则进行请求转换。比如 /api/gateway/server/hello/word 对应的规则为`${gateway.server.prefix:/api/gateway/server/helloword}/${ServiceName:hello}/${methodName:word}`
ServiceName(HellowordService->HelloWord)会默认去掉Service
3. 通过ServiceName找到相应的BindableService。通过methodName找到相应的method.然后通过反射调用接口
4. 输出返回到Http请求


### GrpcClientGateway
此项目是用来配置GrpcClient层的Json转Grpc请求的
和Server的区别不同的是。实现不在这一层。这一层只有调用类

比如你项目DemoProject提供了proto文件为
```proto
   message WordReq{}
   message WordRsp{}
   service HelloService{
       rpc Word(WordReq)returns(WordRsp)
   }
```
那你可以通过
```shell
     curl -H "Content-Type: application/json" -X POST \
         -d '{}' "http://localhost/api/gateway/client/demo/hello/word"
```
获得相应的接口返回

首先接入如下配置
```yaml
grpc:
  gateway:
    client:
      enable: true
      prefix: /api/gateway/client/*
  client:
    demo-project:
      address: 'static://127.0.0.1:19080'
      enableKeepAlive: true
      keepAliveWithoutCalls: true
      negotiationType: plaintext
      gateway:
        prefix: demo
        enable: true
        package: com.hello.word
```
1. grpc.gateway.client.prefix 是前缀.每一个client项目都有gateway配置。     
2. prefix(grpc.client.demo-project.gateway.prefix:demo)为模块的第一个moduleName.假如没有。会默认填配置的名字(grpc.client.demo-project:demo-project)   
3. enable(grpc.client.demo-project.gateway.enable)设置为true后才会开启gateway.    
4. package是扫描的Grpc_Proto的数据类包名.一般来说是为proto文件的package选项。必须填写   

###  原理
1. 读取配置grpc.clients  
2. 通过固定规则进行请求转换。比如 /api/gateway/client/demo/hello/word 对应的规则为`${gateway.client.prefix}/${moduleName}/${ServiceName}/${methodName}`  
3. 扫描相应的package获得相应的BlockingStubClass.并通过配置项获得Channel.动态的生成相应的BlockingStub。并生成相应的HttpServlet  
4. 返回的数据写回到http请求中   

### 问题
假如出现了404的ERROR。但是根本就没有调用到这个Post服务
确认下是不是加载了spring-security。这样需要禁用csrf服务
