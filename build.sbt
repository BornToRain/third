import Dependencies.{akka, circe, other, rxmongo}

name                       := "third"
version       in ThisBuild := "1,0"
scalaVersion  in ThisBuild := "2.12.6"
organization  in ThisBuild := "com.oasis.third"
scalacOptions in ThisBuild := Seq(
  "-encoding", "UTF-8",
  "-Ypartial-unification",
  "-opt-warnings",
  "-deprecation",
  "-opt:l:inline",
  "-feature"
)

/*******************框架*******************/
//核心组件
lazy val `core` = (project in file("core"))
.settings(
  libraryDependencies ++= Seq(
    akka.actor, akka.slf4j,
    akka.http, akka.stream,
    akka.clusterShard, akka.clusterMetrics, akka.clusterTools,
    circe.parser, circe.genericExtras, circe.java8,
    other.logback, other.redis % "provided", rxmongo.rxmongo % "provided"
  )
)
.dependsOn(`protocol`)
//协议
lazy val `protocol` = (project in file("protocol"))
.settings(
  libraryDependencies  ++= Seq(
    other.protobuf
  ),
  PB.targets in Compile := Seq(
    scalapb.gen() -> (sourceManaged in Compile).value
  )
)
/*******************框架*******************/

//金管家
lazy val `pa`     = project in file("pa")
//打电话
lazy val `call`   = project in file("call")
//发短信
lazy val `sms`    = project in file("sms")
//微信公众号
lazy val `wechat` = project in file("wechat")

addCommandAlias("calla", ";project call-core;assembly")
addCommandAlias("smsa", ";project sms-core;assembly")
addCommandAlias("paa", ";project pa-core;assembly")
addCommandAlias("wechata", ";project wechat-core;assembly")
addCommandAlias("corea", ";project core;assembly")