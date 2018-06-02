package org.ryze.micro.core.actor

import java.util.Optional

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.cluster.sharding.ShardRegion.HashCodeMessageExtractor
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import com.typesafe.config.Config
import org.ryze.micro.core.domain.DomainName
import org.ryze.micro.protocol.domain.DomainCommand

/**
  * Actor工厂
  */
case class ActorFactory(config: Config)
{
  implicit val system  = ActorSystem(config getString "akka.cluster.name", config)
  //Actor运行环境隐式量
  implicit val runtime = ActorRuntime(system)

  lazy val nodes            = config getInt "akka.cluster.max-nodes"
  //Hash分片
  lazy val messageExtractor = new HashCodeMessageExtractor(nodes * 10)
  {
    override def entityId(message: Any) = message match
    {
      case c: DomainCommand => c.id
    }
  }

  /**
    * 创建分片
    */
  @inline
  final def shard(props: Props, shardName: String)(implicit domainName: DomainName) = ClusterSharding(system) start(
    typeName         = shardName,
    entityProps      = props,
    settings         = ClusterShardingSettings(system) withRole domainName.name,
    messageExtractor = messageExtractor
  )
  /**
    * 创建代理
    */
  @inline
  final def proxy(name: String) = ClusterSharding(system) startProxy(
    typeName         = name,
    role             = Optional of name,
    messageExtractor = messageExtractor
  )
  /**
    * 创建集群单例
    */
  @inline
  final def singleton(props: Props, singletonName: String)(implicit domainName: DomainName) = system actorOf(
    props = ClusterSingletonManager props(
      singletonProps     = props,
      terminationMessage = PoisonPill,
      settings           = ClusterSingletonManagerSettings(system) withRole domainName.name
    ),
    name  = singletonName
  )
  /**
    * 获取集群单例
    */
  @inline
  final def getSingleton(singletonName: String)(implicit domainName: DomainName) = system actorOf(
    props = ClusterSingletonProxy props(
      s"/user/$singletonName",
      ClusterSingletonProxySettings(system) withRole domainName.name
    ),
    name  = s"$singletonName-proxy"
  )
}
