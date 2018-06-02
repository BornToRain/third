import Dependencies._

//发短信协议(请求|命令|事件)
lazy val `sms-protocol` = (project in file("protocol"))
.settings(
  name                  := "sms-protocol",
  PB.targets in Compile := Seq(
    scalapb.gen() -> (sourceManaged in Compile).value
  )
)
.dependsOn(LocalProject("protocol"))

//发短信核心实现
lazy val `sms-core` = (project in file("core"))
.settings(
  name                 := "sms-core",
  libraryDependencies ++= Seq(
    akka.persistenceQuery, akka.persistenceRxMongo,
    rxmongo.rxmongo, rxmongo.rxmongoStream
  ),
  mainClass       in assembly := Some("com.oasis.third.sms.SmsAppStartUp"),
  assemblyJarName in assembly := "sms.jar"
)
.dependsOn(`sms-protocol`, LocalProject("core"))