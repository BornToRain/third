import Dependencies._
import sbt.Keys.mainClass
import sbtassembly.AssemblyPlugin.autoImport.assemblyJarName

//平安金管家协议(请求|命令|事件)
lazy val `pa-protocol` = (project in file ("protocol"))
.settings(
  name                  := "pa-protocol",
  PB.targets in Compile := Seq(
    scalapb.gen() -> (sourceManaged in Compile).value
  )
)
.dependsOn(LocalProject("protocol"))

//平安金管家核心实现
lazy val `pa-core` = (project in file ("core"))
.settings(
  name                        := "pa-core",
  libraryDependencies        ++= Seq(
    fp.catsCore,
    other.logback,
    //金管家用
    "commons-codec" % "commons-codec" % "1.11",
    "commons-lang"  % "commons-lang"  % "2.6"
  ),
  mainClass       in assembly := Some("com.oasis.third.pa.PAAppStartUp"),
  assemblyJarName in assembly := "pa.jar",
  addCompilerPlugin(
    "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
  )
)
.dependsOn(`pa-protocol`, LocalProject("core"))
