package org.ryze.micro.core.mongodb

import com.typesafe.config.Config
import org.ryze.micro.core.actor.ActorRuntime
import reactivemongo.api.MongoDriver

import scala.collection.JavaConverters._

/**
  * MongoDB客户端
  */
case class MongoDBClient(config: Config)(implicit runtime: ActorRuntime)
{
  import runtime._

  private[this] val mongodbConfig = config.getConfig("mongodb")

  lazy val driver     = new MongoDriver
  lazy val connection = driver.connection(mongodbConfig.getStringList("servers").asScala)
  lazy val db         = connection(mongodbConfig.getString("database"))
}
