import sbt._

object Version
{
  val akka    = "2.5.12"
  val http    = "10.1.1"
  val circe   = "0.9.3"
  val cats    = "1.1.0"
  val rxmongo = "0.12.6"
}

object Dependencies
{
  object akka
  {
    lazy val actor              = apply("actor")
    lazy val slf4j              = apply("slf4j")
    lazy val stream             = apply("stream")
    //http
    lazy val http               = apply("http", Version.http)
    //集群
    lazy val cluster            = apply("cluster")
    lazy val clusterShard       = apply("cluster-sharding")
    lazy val clusterMetrics     = apply("cluster-metrics")
    lazy val clusterTools       = apply("cluster-tools")
    //持久化
    lazy val persistence        = apply("persistence")
    lazy val persistenceQuery   = apply("persistence-query")
    lazy val persistenceRxMongo = "com.github.scullxbones" %% "akka-persistence-mongo-rxmongo" % "2.0.10"

    @inline
    private[this] def apply(name: String, version: String = Version.akka) = "com.typesafe.akka" %% s"akka-$name" % version
  }

  object circe
  {
    lazy val parser        = apply("parser")
    lazy val genericExtras = apply("generic-extras")

    @inline
    private[this] def apply(name: String) = "io.circe" %% s"circe-$name" % Version.circe
  }

  object fp
  {
    lazy val catsCore = apply("core")
    lazy val catsFree = apply("free")

    @inline
    private[this] def apply(name: String) = "org.typelevel" %% s"cats-$name" % Version.cats
  }

  object rxmongo
  {
    lazy val rxmongo = apply("reactivemongo")
    lazy val rxmongoStream = apply("reactivemongo-akkastream")

    @inline
    private [this] def apply(name: String) = "org.reactivemongo" %% name % Version.rxmongo
  }

  object other
  {
    //日志
    lazy val logback      = "ch.qos.logback"       %  "logback-classic"    % "1.2.3"
    //Protobuf序列化
    lazy val protobuf     = "com.thesamet.scalapb" %% "scalapb-runtime"    % scalapb.compiler.Version.scalapbVersion % "protobuf"
    //Redis
    lazy val redis        = "com.github.etaty"     %% "rediscala"          % "1.8.0"
    lazy val jodaTime     = "joda-time"            %  "joda-time"          % "2.9.9"
  }
}
