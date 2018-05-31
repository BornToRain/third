package org.ryze.micro.core.cluster

import java.util.Optional

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.cluster.sharding.ShardRegion.HashCodeMessageExtractor
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import org.ryze.micro.protocol.domain.DomainCommand

/**
  * 集群工厂
  */
case class ClusterFactory(maxShard: Int)(implicit system: ActorSystem)
{
  //Hash分片
  private[this] val messageExtractor = new HashCodeMessageExtractor(maxShard)
  {
    override def entityId(message: Any) = message match
    {
      case c: DomainCommand => c.id
    }
  }

  //创建分片
  @inline
  def createShard(props: Props)(shardName: String)(appName: String) = ClusterSharding(system).start(
    typeName         = shardName,
    entityProps      = props,
    settings         = ClusterShardingSettings(system) withRole appName,
    messageExtractor = messageExtractor
  )
  //创建代理
  @inline
  def createProxy(name: String) = ClusterSharding(system).startProxy(
    typeName         = name,
    role             = Optional.of(name),
    messageExtractor = messageExtractor
  )
  //创建单例
  @inline
  def createSingleton(props: Props)(singletonName: String)(appName: String) = system actorOf(
    props = ClusterSingletonManager props(
      singletonProps     = props,
      terminationMessage = PoisonPill,
      settings           = ClusterSingletonManagerSettings(system) withRole appName
    ),
    name = singletonName
  )
  //获取单例
  @inline
  def getSingleton(singletonName: String)(appName: String) = Option
  {
    system.actorOf(
      props = ClusterSingletonProxy.props(
        s"/user/$singletonName",
        ClusterSingletonProxySettings(system) withRole appName
      ),
      name  = s"$singletonName-proxy"
    )
  }
  //获取分片
  @inline
  def getShard(shardName: String) = try
  {
    Some(ClusterSharding(system) shardRegion shardName)
  }
  catch
  {
    case e: Exception => None
  }
  //注册客户端
  @inline
  def registerClientReceptionist(name: String): Unit = ClusterClientReceptionist(system) registerService getShard(name).get
  //查询分片,无则创建.并注册客户端
  @inline
  def shard(props: Props)(shardName: String)(appName: String) =
  {
    val ref = getShard(shardName) getOrElse createShard(props)(shardName)(appName)
    registerClientReceptionist(shardName)
    ref
  }
  //查询代理,无则创建,并注册客户端
  @inline
  def proxy(name: String) =
  {
    val ref = getShard(name) getOrElse createProxy(name)
    registerClientReceptionist(name)
    ref
  }
  //查询单例,无则创建
  @inline
  def singleton(props: Props)(singletonName: String)(appName: String) =
  {
    val ref = getSingleton(singletonName)(appName) getOrElse createSingleton(props)(singletonName)(appName)
    ref
  }
}
