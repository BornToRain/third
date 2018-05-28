package org.ryze.micro.core.actor

import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.ryze.micro.core.cluster.ClusterFactory

/**
  * Actor工厂
  */
case class ActorFactory(config: Config)
{
  implicit val system  = ActorSystem(config.getString("akka.cluster.name"), config)
  //Actor运行环境隐式量
  implicit val runtime = ActorRuntime(system)
  //Actor集群
  lazy val cluster = ClusterFactory(config.getInt("akka.cluster.max-nodes") * 10)
}
