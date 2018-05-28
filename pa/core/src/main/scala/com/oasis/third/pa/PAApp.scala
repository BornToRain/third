package com.oasis.third.pa

import akka.actor.{Props, Terminated}
import com.oasis.third.pa.infrastructure.service.{OasisClient, PAClient}
import com.oasis.third.pa.interfaces.api.v1.PAApi
import com.typesafe.config.ConfigFactory
import org.ryze.micro.core.actor.{ActorFactory, ActorL}
import org.ryze.micro.core.redis.RdsClient
import org.ryze.micro.core.tool.ConfigLoader

import scala.annotation.switch

object PAAppStartUp extends App with ConfigLoader
{
  //种子节点
  val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=2551")
    .withFallback(loader)

  implicit val factory = ActorFactory(config)

  factory.system actorOf(PAApp.props, PAApp.NAME)
}

/**
  * 平安金管家模块
  */
class PAApp(implicit factory: ActorFactory) extends ActorL
{
  import factory.runtime

  private[this] val redis  = RdsClient(factory.config).rds
  private[this] val client = PAClient(redis)
  private[this] val oasis  = OasisClient()
  private[this] val api    = context actorOf (PAApi.props(client, oasis),PAApi.NAME)

  context watch api

  override def receive =
  {
    case Terminated(ref) => log.info(s"Actor已经关闭: ${ref.path}")
      context.system.terminate
  }
}

object PAApp
{
  final val NAME = "pa-app"

  def props(implicit factory: ActorFactory) = Props(new PAApp)
}