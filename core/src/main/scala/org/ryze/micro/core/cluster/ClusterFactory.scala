package org.ryze.micro.core.cluster

import java.util.Optional

import akka.actor.{ActorSystem, Props}
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.cluster.sharding.ShardRegion.HashCodeMessageExtractor
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
  def createShard(name: String)(props: Props) = ClusterSharding(system).start(
    typeName         = name,
    entityProps      = props,
    settings         = ClusterShardingSettings(system).withRole(name),
    messageExtractor = messageExtractor
  )
  //创建代理
  def createProxy(name: String) = ClusterSharding(system).startProxy(
    typeName         = name,
    role             = Optional.of(name),
    messageExtractor = messageExtractor
  )
  //获取分片
  def getShard(name: String) = try
  {
    Some(ClusterSharding(system) shardRegion name)
  }
  catch
  {
    case e: Exception => None
  }
  //注册客户端
  @inline
  def registerClientReceptionist(name: String): Unit = ClusterClientReceptionist(system) registerService getShard(name).get
  //查询分片,无则创建.并注册客户端
  def shard(name: String)(props: Props) =
  {
    val ref = getShard(name) getOrElse createShard(name)(props)
    registerClientReceptionist(name)
    ref
  }
  //查询代理,无则创建,并注册客户端
  def proxy(name: String) =
  {
    val ref = getShard(name) getOrElse createProxy(name)
    registerClientReceptionist(name)
    ref
  }
}
