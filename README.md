# [temporary](http://60.205.106.190:3000/ryze/temporary) [![Build Status](https://travis-ci.org/idugalic/micro-company.svg?branch=master)](http://60.205.106.190:3000/ryze/temporary)

泓华临时项目(容联七陌、阿里短信、微信支付、公众号)

## 项目架构
 
``` scala
temporary
├── application.service -- 应用服务层
├── domain -- 领域服务层
    ├── call -- 容联七陌打电话
    ├── menu -- 微信菜单 
    ├── payment -- 微信支付
    ├── sms -- 阿里云短信
├── infrastructure -- 基础设施层
    ├── repository -- 领域层仓储实现
    ├── service -- 基础服务(redis、微信、MongoDB各种客户端)
    ├── tool -- 常用工具类(雪花ID、Totp验证码)
├── interfaces -- 接口层 
    ├── api.v2 -- 接口
    ├── dto -- 微信公众号DTO
├── main -- 项目入口
```

### 技术选型

#### 开发技术:
技术 | 描述 | 官网
----|------|----
Scala | JVM下FP为主OOP为辅语言  | [https://www.scala-lang.org/](https://www.scala-lang.org/)
Akka | 高并发、分布式和容错应用的工具包 | [https://akka.io/](https://akka.io/)
Sbt | 项目构建管理  | [https://www.scala-sbt.org/](https://www.scala-sbt.org/)
Redis | NoSQL | [https://redis.io/](https://redis.io/)
MongoDB | NoSQL | [https://www.mongodb.com/](https://www.mongodb.com/)
... | ... |


#### 开发环境：
- Docker
- JDK8+
- Scala2.12+
- MongoDB
- Sbt

#### 开发工具:
- MongoDB: 数据库
- Akka: 应用服务器
- Git: 版本管理
- Nginx: 反向代理服务器
- IntelliJ IDEA: 开发IDE

#### 开发进度

- [x] 搭建架构
- [x] 短信模块
- [x] 公众号模块
- [x] 容联七陌模块
- [ ] 微信支付模块
