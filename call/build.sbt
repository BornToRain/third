import Dependencies._
import sbtassembly.AssemblyPlugin.autoImport.assemblyJarName

//打电话协议(请求|命令|事件)
lazy val `call-protocol` = (project in file("protocol"))
.settings(
  name                  := "call-protocol",
  PB.targets in Compile := Seq(
    scalapb.gen() -> (sourceManaged in Compile).value
  )
)
.dependsOn(LocalProject("protocol"))

//打电话核心实现
lazy val `call-core` = (project in file("core"))
.settings(
  name                        := "call-core",
  libraryDependencies        ++= Seq(
    other.redis,
    akka.persistenceQuery, akka.persistenceRxMongo,
    rxmongo.rxmongo, rxmongo.rxmongoStream
  ),
  mainClass       in assembly := Some("com.oasis.third.call.CallAppStartUp"),
  assemblyJarName in assembly := "call.jar"
)
.dependsOn(`call-protocol`, LocalProject("core"))