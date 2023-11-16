# raf-framework 2.0.0
本框架基于如下构建
```
Spring Boot Version: 2.7.14.RELEASE
Spring Cloud Version: 2021.0.5
Spring Cloud Alibaba Version: 2021.0.5.0
```
可以灵活采用基于SpringCloud Feign或SpringCloud Alibaba Dubbo构建微服务体系

## 目的
简化微服务的使用，简化常用组件的使用，统一编码风格，用最佳实践使用各种组件。
使开发更关注业务本身开发，提升开发效率。后期组件版本修改，组件替换更加灵活。
统一的框架底层，更能迅速适应高性能，高并发，高可用，高安全的互联网时代。

## 使用条件
```
JDK 1. 8+
Maven 3.5.2+
```

## 设计原则
1. 组件支持热插拔，遵循引用才使用的原则
2. 开关模式，大部分都默认关闭，可以灵活选择是否启用
3. 底层不封装任何硬代码，包括地址，秘钥等
4. 统一的编码风格，符合代码扫描规范
5. 依赖第三方包无重大安全漏洞，自身代码无重大安全漏洞
6. maven插件只封装基础的打包功能，其它像代码检查，Java doc生成走第三方工具，解耦
7. 采用flatten-maven-plugin插件，revision占位符统一版本
8. 启动日志清爽，引用jar清爽，打包的jar清爽

## 封装的内容

1. 统一的异常处理，系统异常，组件异常，业务异常等
2. 通用的返回结果,枚举,常量,错误码
3. 统一的入参，出参格式
4. 统一的分页格式
5. 统一的公共类库
6. 接口多版本
7. 跨域配置
8. 统一的http请求输入，输出日志配置
9. swagger配置
10. 异步线程池：参数支持上报prometheus实现监控，或者清空队列等
11. 统一日志输出格式，方便采集，统计，分析
    采用logback，异步日志，默认路径/data/logs，支持路径修改
    日志格式（co-demo/2019-10-17/application_16.log）按小时切割，单文件最大200M，默认183天
    DEBUG,INFO,WARN,ERROR
    所有日志一个文件存储，不重复记录
    DEBUG默认不开启，调试才能开启,INFO默认日志级别,WARN必须减少数量,ERROR日志必须消灭掉
    敏感信息脱敏走业务方式
    日志格式统一，不要重复记录日志
12. jasypt: 敏感配置信息加密
13. jackson：高性能,统一的序列化，反序列化使用方式
14. 优雅停机 ：默认处理30s，delete 注册中心&kill xxx，30s后强制kill -9
15. 分布式日志追踪，dubbo，feign，异步线程
16. nacos：服务发现，配置中心
17. mybatis：集成Hikari，相对其它数据源有更高的性能，支持多数据源，监控
18. dubbo：服务调用，全局异常处理，分布式日志追踪
19. rabbmitmq：普通队列，死信队列，网络抖动重发
20. sentry：日志监控平台
21. okhttp：统一第三方api调用
22. redis：引入Lettuce有更高的性能，统一序列化方式，缓存，redisson分布式锁
23. monitor：actuator监控，micrometer

## 框架介绍
```
raf-framework(聚合，仓库信息介绍）
raf-framework-dependencies（配置管理，版本管理,插件管理）
raf-framework-parent（框架父工程）
raf-framework-starter（聚合）
raf-framework-redis-starter（组件）

raf-framework(聚合，仓库信息，介绍）
raf-framework-dependencies（配置管理，版本管理,插件管理）
raf-framework-parent（框架父工程）
raf-framework-autoconfigure（基本库）

raf-framework-dependencies
raf-framework-starter-parent（所有新项目父工程)

#Starter组件
raf-framework-springmvc-starter (基础starter，jasypt)
raf-framework-cloud-starter（基于springcloud feign调用构建微服务）
raf-framework-sleuth-starter
raf-framework-dubbo-starter（基于dubbo调用构建微服务）
raf-framework-nacos-config-starter
raf-framework-nacos-discovery-starter
raf-framework-mybatis-starter (依赖raf-framework-jdbc-starter)
raf-framework-redis-starter
raf-framework-okhttp-starter
raf-framework-rabbit-starter
raf-framework-monitor-starter
raf-framework-sentry-starter
raf-framework-swagger-starter
raf-framework-starter-parent（新项目父工程）
```

## 如何使用 (参考示例项目:co-demo)
1. 新建springboot项目
   groupId命名规则: com.公司简称.部门或业务线简称(com.co.xx)
   artifactId命名规则：项目名称.业务模块名称(co-demo)

2. parent依赖
```
<!--raf starter parent-->
<parent>
    <groupId>com.github.raf</groupId>
    <artifactId>raf-framework-starter-parent</artifactId>
    <version>2. 0.0-SNAPSHOT</version>
    <relativePath/>
</parent>

<dependencies>
    <!--raf spring mvc-->
    <dependency>
        <groupId>com.github.raf</groupId>
        <artifactId>raf-framework-springmvc-starter</artifactId>
        <version>[2. 0.0-SNAPSHOT,)</version>
    </dependency>
</dependencies>
```

3. 服务实现无需新增maven相关插件，facade工程需要禁用父类的插件行为
```
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <!-- 禁用父项目插件 -->
            <executions>
                <execution>
                    <phase>none</phase>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 组件使用方法
### nacos使用方法
1. 启动类新增

```
@EnableDiscoveryClient
```

2. 添加依赖

```
<!--raf nacos config-->
<dependency>
    <groupId>com.github.raf</groupId>
    <artifactId>raf-framework-nacos-config-starter</artifactId>
    <version>[2. 0.0-SNAPSHOT,)</version>
</dependency>

<!--raf nacos discovery-->
<dependency>
    <groupId>com.github.raf</groupId>
    <artifactId>raf-framework-nacos-discovery-starter</artifactId>
    <version>[2. 0.0-SNAPSHOT,)</version>
</dependency>
```

3. 新增配置
```
bootstrap.yml

nacos:
  addr: ${NACOS_ADDR:127.0.0.1:8890}
  namespace: ${NACOS_NAMESPACE:}

#新增nacos配置中心
spring:
  cloud:
    nacos:
      config:
        enabled: true
        fileExtension: yml
        serverAddr: ${nacos.addr}
        namespace: ${nacos.namespace}
        prefix: ${spring.application.name}
        group: default
        sharedConfigs:
          - dataId: base.yml
            group: default
            refresh: true
        extensionConfigs:
          - dataId: ${spring.application.name}-ext.yml
            group: default
            refresh: true

#新增nacos服务发现
spring:
  cloud:
    nacos:
      discovery:
        server-addr: ${nacos.addr}
        namespace: ${nacos.namespace}
```

### dubbo使用方法
1. 添加依赖

```
<!--raf dubbo-->
<dependency>
    <groupId>com.github.raf</groupId>
    <artifactId>raf-framework-dubbo-starter</artifactId>
    <version>[2. 0.0-SNAPSHOT,)</version>
</dependency>
```

2. 添加配置
```
#dubbo配置，支持dubbo3.x 应用或者服务方式，dubbo，grpc协议
dubbo:
 application:
   name: ${spring.application.name}
   qos-enable: false
   register-mode: service  #all，instance，service
   metadata-type: remote
   serialize-check-status: DISABLE
   check-serializable: false
 registry:
   address: nacos://${nacos.addr}?namespace=${nacos.namespace} #nacos://${nacos.addr}?registry-type=service
   protocol: grpc
   timeout: 2000
   register-mode: FORCE_INTERFACE #APPLICATION_FIRST，FORCE_APPLICATION，FORCE_INTERFACE
   file: ../registry/${dubbo.application.name}
  #  parameters:
  #    namespace: ${nacos.namespace}
  #    register-consumer-url: false
   use-as-metadata-center: true
   use-as-config-center: false
 protocol:
   name: grpc
   port: 28001
   corethreads: 200 #核心线程数
   serialization: hessian2
 provider:
   actives: 0 #consumer，每方法最大并发调用数
   connections: 10 #consumer，最大并发调用数
   accepts: 0 #最大可以接受的连接数
   threadpool: cached
   threads: 300 #最大线程数（队列为0是阻塞对列，该值没用）
   alive: 5000 #非核心线程存活时间
   queues: 0 #阻塞对列
   timeout: 60000
   executes: 0 #提供者,每方法最大并发调用数
   retries: 0
   filter: -exception
 consumer:
   check: false
   retries: 0
```

### mybatis使用方法
1. 添加依赖
```
<!--mybatis-->
<dependency>
    <groupId>com.github.raf</groupId>
    <artifactId>raf-framework-mybatis-starter</artifactId>
    <version>[2. 0.0-SNAPSHOT,)</version>
</dependency>
```

2. 添加配置
```
#mybatis配置
raf.dataSource:
  oms:
    url: jdbc:mysql://${db.addr}/co_oms_db?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&failOverReadOnly=false&useSSL=false&allowMultiQueries=true&tinyInt1isBit=false&serverTimezone=Asia/Shanghai
    username: ${db.username}
    password: ${db.password}
    mapperLocations: mapper/oms/*.xml
    basePackage: com.company.a.dao.oms
    typeAliasesPackage: com.company.a.entity
    driver-class-name: org.mariadb.jdbc.Driver
    configLocation: classpath:mapper/mybatis-config.xml
    pool:
      maximumPoolSize: 10 #cpu数*2+磁盘数
      idleTimeout: 600000 #默认10分钟
      connectionTimeout: 15000 #15s比数据库大，数据库10s
      maxLifetime: 1740000 #29分钟比数据库小，数据库30
  cms:
    url: jdbc:mysql://${db.addr}/co_cms_db?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&failOverReadOnly=false&useSSL=false&allowMultiQueries=true&tinyInt1isBit=false&serverTimezone=Asia/Shanghai
    username: ${db.username}
    password: ${db.password}
    mapperLocations: mapper/cms/*.xml
    basePackage: com.company.a.dao.cms
    typeAliasesPackage: com.company.a.entity
    driver-class-name: org.mariadb.jdbc.Driver
    configLocation: classpath:mapper/mybatis-config.xml
    pool:
      maximumPoolSize: 10 #cpu数*2+磁盘数
      idleTimeout: 600000 #默认10分钟
      connectionTimeout: 15000 #15s比数据库大，数据库10s
      maxLifetime: 1740000 #29分钟比数据库小，数据库30

log4jdbc.sqltiming:
  warn.threshold: 300   #300ms 会打印warn级别的日志,sqltiming:info才生效
  error.threshold: 500  #500ms 会打印error级别的日志

raf.pagehelper:
  helperDialect: "mysql"
  offsetAsPageNum: false
  rowBoundsWithCount: false
  pageSizeZero: false
  reasonable: false
  params: "pageNum=pageNum;pageSize=pageSize;count=countSql;reasonable=reasonable;pageSizeZero=pageSizeZero"
  supportMethodsArguments: false
  autoRuntimeDialect: false
  closeConn: true
```

### redis使用方法
1. 添加依赖
```
<!--redis-->
<dependency>
    <groupId>com.github.raf</groupId>
    <artifactId>raf-framework-redis-starter</artifactId>
    <version>[2. 0.0-SNAPSHOT,)</version>
</dependency>
```

2. 添加配置
```
#redis配置
spring.redis:
  database: 0
  host: 127.0.0.1
  port: 6379
  password:
  timeout: 2s
  lettuce:
    pool:
      max-active: 10 #连接池最大连接数（使用负值表示没有限制）
      max-idle: 10 #连接池中的最大空闲连接
      min-idle: 0 #连接池中的最小空闲连接
      max-wait: 2s #连接池最大阻塞等待时间（使用负值表示没有限制）
#    cluster.nodes:
#      - 0.0.0.0:0

spring.cache:
  redis:
    time-to-live: 86400s #默认过期时间一天
    cacheNullValues: false
  cache-names:
  - user_cache #针对具体key设置过期时间
  - user_list_cache

raf.customCache:
  user_cache:
    time-to-live: 200s #对应上面key
  user_list_cache:
    time-to-live: 100s
```

### okHttp使用方法
1. 添加依赖
```
<!--okhttp-->
<dependency>
    <groupId>com.github.raf</groupId>
    <artifactId>raf-framework-okhttp-starter</artifactId>
    <version>[2. 0.0-SNAPSHOT,)</version>
</dependency>
```

2. 添加配置
```
#okhttp配置
raf.okhttp:
  connectTimeout: 2000
  readTimeout: 2000
  writeTimeout: 2000
  retryOnConnectionFailure: true
  followRedirects: true
  followSslRedirects: true
  connection:
    maxIdleConnections: 10
    keepAliveDuration: 30000
  level: BASIC #日志级别 NONE BASIC HEADERS BODY
```

### rabbit mq使用方法
1.  添加依赖
```
<!--rabbit mq-->
<dependency>
    <groupId>com.github.raf</groupId>
    <artifactId>raf-framework-rabbit-starter</artifactId>
    <version>[2. 0.0-SNAPSHOT,)</version>
</dependency>
```

2.  添加配置
```
#rabbitMq配置
raf.rabbit:
  enabled: true
  addresses: ${mq.addr}
  username: ${mq.username}
  password: ${mq.password}
  consumer: #消费者配置
    group: ${spring.application.name} #消费者组,默认写spring.application.name即可
  provider: #生产者配置
    exchange: xx.demo.topic #topicExchange交换机名称
    ack: true #是否监听发送返回 confirm，return
  delay: #延迟队列，同一个业务延迟时间间隔必须一样
    deadExchange: xx.demo.dead.topic #死信队列
    receiveExchange: xx.demo.receive.topic #接收队列
    queuePrefix:
      - xx.demo.delay.businessOne #业务1 接收队列xx.demo.delay.businessOne.receive.queue
      - xx.demo.delay.businessTwo #业务2 接收队列xx.demo.delay.businessTwo.receive.queue
```

### swagger使用方法
1. 启动类添加
```
@EnableSwagger2Doc
```
2. 添加依赖
```
<!--raf swagger-->
<dependency>
    <groupId>com.github.raf</groupId>
    <artifactId>raf-framework-swagger-starter</artifactId>
    <version>[2. 0.0-SNAPSHOT,)</version>
</dependency>
```

3. 添加配置

```
#swagger配置
swagger:
  enabled: true
  contact:
    name: xxx
    email: xx@xx.com
  authorization:#token
    name: Authorization
    type: ApiKey
    key-name: Authorization
    auth-regex: ^.*$
  globalOperationParameters:#以下参数只限原子服务使用
  - name: x-user-id
    modelRef: string
    parameterType: header
    required: false
    description:
  - name: x-user-name-header
    modelRef: string
    parameterType: header
    required: false
    description:
  - name: x-role-header
    modelRef: string
    parameterType: header
    required: false
    description:
  docket:
    all:
      version: v-all
      title: demo项目接口文档
      description: demo项目接口文档
      base-package: com.company.demo.controller
      base-path: /**
```

4. 访问地址：http://xxx/doc.html 或 /swagger-ui/index.html

### spring cloud eureka使用方法

1. 添加依赖

```
<!--spring cloud eureka-->
<dependency>
    <groupId>com.github.raf</groupId>
    <artifactId>raf-framework-eureka-starter</artifactId>
    <version>[2. 0.0-SNAPSHOT,)</version>
</dependency>
```
2. 启动类添加
```
@EnableDiscoveryClient
```

3. 添加配置

```
eureka:
  client:
    registry-fetch-interval-seconds: 10 #拉取注册表（默认30s）
    serviceUrl:
      defaultZone: http://eureka:eka321@localhost:1111/eureka/
  instance:
    instance-id: ${spring.cloud.client.ip-address}:${server.port}
    prefer-ip-address: true
    status-page-url-path: /swagger-ui.html
    lease-renewal-interval-in-seconds: 10 #服务续约间隔时间（缺省为30s）
    lease-expiration-duration-in-seconds: 30 #服务续约到期时间（默认90s）
```


### spring-cloud使用方法
1. 添加依赖
```
<!--spring cloud-->
<dependency>
    <groupId>com.github.raf</groupId>
    <artifactId>raf-framework-cloud-starter</artifactId>
    <version>[2. 0.0-SNAPSHOT,)</version>
</dependency>
```

2. 启动类添加
```
@EnableFeignClients
```
3. 添加配置
```
#熔断配置
hystrix:
  command:
    default:
      execution:
        isolation:
          thread:
            timeoutInMilliseconds: 60000

#ribbon配置
ribbon:
  ConnectTimeout: 1000 # 请求连接超时时间
  ReadTimeout: 2000 # 请求处理的超时时间
  OkToRetryOnAllOperations: false
  MaxAutoRetries: 0 # 对当前实例的重试次数
  MaxAutoRetriesNextServer: 0 # 切换实例的重试次数
  okhttp:
    enabled: true

#feign配置
feign:
  hystrix:
    enabled: true

#feign重试
raf.feign-retry:
  enabled: false
  max-attempts: 5
  
#sleuth配置
spring:
  zipkin:
    enabled: false
    base-url: http://localhost:9992
  sleuth:
    enabled: true
    sampler:
      probability: 1. 0
```

### sentry使用
错误日志实时跟踪服务
1. 添加依赖
```
<!--raf sentry-->
<dependency>
    <groupId>com.github.raf</groupId>
    <artifactId>raf-framework-sentry-starter</artifactId>
    <version>[2. 0.0-SNAPSHOT,)</version>
</dependency>
```

2. 添加配置
```
#sentry配置
raf.sentry:
  enabled: true #默认false
  dns: https://f488ea8838b547c5a212c6d0faff312b@sentry.io/1783484?stacktrace.app.packages=xx-demo&environment=dev
```

### nacos敏感信息加密使用
采用jasypt对敏感配置信息进行统一的加解密

input：明文/密文；password：秘钥
java -cp jasypt-1. 9.3.jar org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI input='xxxx' password='xxxx' algorithm=PBEWITHHMACSHA512ANDAES_256 ivGeneratorClassName=org.jasypt.iv.RandomIvGenerator
java -cp jasypt-1. 9.3.jar org.jasypt.intf.cli.JasyptPBEStringDecryptionCLI input='xxxx' password='xxxx' algorithm=PBEWITHHMACSHA512ANDAES_256 ivGeneratorClassName=org.jasypt.iv.RandomIvGenerator

在nacos配置使用
1. 配置秘钥
   jasypt.encryptor.password=xxxx

2. 使用加密后的值
   xxx.pwd=ENC(gVSS/lDEq/J8RG4Af48mPbcPReadKnXXXXkOprEh/Hghn1paGZHfiqUPj4lnQDtt)

### 其它配置使用
```
#日志配置
logging:
  level:
    root: info
    com.company.a.dao.cms: info
    jdbc:
      sqlonly: OFF
      sqltiming: INFO
      audit: OFF
      resultset: OFF
      resultsettable: OFF
      connection: OFF
  file:
    path: /data/logs

#日志记录配置（输入，输出）
#BASIC,REQ_HEADERS, REQ_BODY, RSP_HEADERS（默认）, RSP_BODY
raf.log:
  enabled: true
  level: REQ_BODY

#优雅停机
raf.tomcat:
  shutdown:
    enabled: true #默认true
    waitTime: 30 #默认30s的优雅停机等待时间，超过时间强行关闭

#跨域配置
raf.cors:
  enabled: false #默认false
  allowOrigins: #默认*, 可以通过数组进行添加
  allowHeaders: #默认*, 可以通过数组进行添加
  allowMethods: #默认*, 可以通过数组进行添加
  allowExposeHeaders: #默认无，可以通过数组进行添加

#异步线程池配置
raf.executor:
  enabled: true
  corePoolSize: 10
  maxPoolSize: 20
  queueCapacity: 100
  keepAliveSeconds: 5
  threadNamePrefix: raf-async-thread-pool-
  metricEnabled: false

#雪花算法
raf.snow-flake:
  enabled: false
```

## 统一http状态码，业务code码
```
http状态码
200 成功
{
  "code": 200,
  "msg": "Success",
  "data": {
    "account": "xxx",
    "email": "xxx",
    "sex": 1,
    "status": 1
  }
}
401 认证错误
{
  "code": 401,
  "msg": "Unauthorized"
}
403 认证通过无权限
{
  "code": 403,
  "msg": "Forbidden"
}
404 资源不存在
{
  "code": 404,
  "msg": "Not Found"
}
405 Method Not Allowed
{
  "code": 405,
  "msg": "Method Not Allowed"
}
415 Unsupported Media Type
{
  "code": 415,
  "msg": "Unsupported Media Type"
}
426 签名错误
{
  "code": 426,
  "msg": "Sign Error"
}
500 服务端错误
{
  "code": 500,
  "msg": "服务端错误，请稍后再试"
}
400 Bad Request
1. 参数校验错误 code:700
{
  "code": 700,
  "msg": "pageNum参数校验错误"
}
{
  "code": 700,
  "msg": "参数校验错误",
  "data": {
    "password": "长度需要在6和30之间",
    "email": "email不能为空"
  }
}
2. 业务异常-友好提示 code：10000以上
{
  "code": 10001,
  "msg": "Xxx失联了，换一个试试吧"
}
```

## 启动部署

```
<layers>
    <enabled>true</enabled>
</layers>
```

由于SpringBoot 2.3.x 已经支持打包分层，所以maven插件已强制配置分层打包

```
推送公共包
mvn clean deploy -Dmaven.test.skip=true
服务打包
mvn clean package -Dmaven.test.skip=true
```

## springboot，springcloud，spring-cloud-alibaba版本依赖关系
https://spring.io/projects/spring-cloud#overview
https://github.com/alibaba/spring-cloud-alibaba/wiki/%E7%89%88%E6%9C%AC%E8%AF%B4%E6%98%8E

## License
Raf Framework is Open Source software released under the MIT license.
