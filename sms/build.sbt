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

lazy val `sms-core` = (project in file("core"))
.settings(
  name                 := "sms-core",
  libraryDependencies ++= Seq(
    other.logback,
    akka.persistence, akka.persistenceQuery, akka.persistenceRxMongo,
    rxmongo.rxmongo, rxmongo.rxmongoStream
  )
)
.dependsOn(`sms-protocol`, LocalProject("core"))