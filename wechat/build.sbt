import Dependencies._
import sbtassembly.AssemblyPlugin.autoImport.PathList

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
  assemblyMergeStrategy in assembly :=
  {
    case PathList(xs @ _*) if (xs.last endsWith "XmlPullParser.class") || (xs.last endsWith "XmlPullParserException.class") => MergeStrategy.first
    case x                                                                                                                  =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)
.dependsOn(LocalProject("core"))

