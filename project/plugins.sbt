addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.18")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")

//scalapb => scala protobuf插件
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.7.4"
