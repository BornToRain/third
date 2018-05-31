package com.oasis.third.pa

import akka.actor.{Props, Terminated}
import com.oasis.third.pa.infrastructure.service.{OasisClient, PAClient}
import com.oasis.third.pa.interfaces.api.v1.PAApi
import com.typesafe.config.ConfigFactory
import org.ryze.micro.core.actor.{ActorFactory, ActorL}
import org.ryze.micro.core.tool.ConfigLoader

import scala.language.postfixOps

object PAAppStartUp extends App with ConfigLoader
{
  //种子节点
  val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=2551")
    .withFallback(loader)

  implicit val factory = ActorFactory(config)

  factory.system actorOf (PAApp.props, PAApp.NAME)
}

/**
  * 平安金管家模块
  */
class PAApp(implicit factory: ActorFactory) extends ActorL
{
  import factory.runtime

  private[this] val client = factory.cluster.createSingleton(PAClient.props)(PAClient.NAME)(APP)
  private[this] val oasis  = context actorOf (OasisClient.props, OasisClient.NAME)
  private[this] val api    = context actorOf (PAApi.props(client, oasis),PAApi.NAME)

  Seq(client, oasis, api) foreach (context watch)

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