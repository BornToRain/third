import Dependencies._

//微信核心实现
lazy val `wechat-core` = (project in file ("core"))
.settings(
  name                        := "wechat-core",
  libraryDependencies        ++= Seq(
    akka.httpXML,
    other.xstream
  ),
  mainClass       in assembly := Some("com.oasis.third.wechat.WechatAppStartUp"),
  assemblyJarName in assembly := "wechat.jar",
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
)
.dependsOn(LocalProject("core"))