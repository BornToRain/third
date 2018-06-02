package com.oasis.third.wechat

import akka.actor.{Props, Terminated}
import com.oasis.third.wechat.infrastructure.service.{PaymentClient, WechatClient}
import com.oasis.third.wechat.interfaces.api.v1.WechatApi
import org.ryze.micro.core.actor.{ActorFactory, ActorL}
import org.ryze.micro.core.tool.ConfigLoader

import scala.language.postfixOps

object WechatAppStartUp extends App with ConfigLoader
{
  implicit val factory = ActorFactory(loader)

  factory.system actorOf (WechatApp props, WechatApp.NAME)
}

class WechatApp(implicit factory: ActorFactory) extends ActorL
{
  import factory.runtime

  private[this] val payment = context actorOf (PaymentClient props, PaymentClient.NAME)
  private[this] val client  = factory singleton (WechatClient props, WechatClient.NAME)
  private[this] val api     = context actorOf (WechatApi props (client, payment), WechatApi.NAME)

  Seq(payment, client, api) foreach (context watch)

  override def receive =
  {
    case Terminated(ref) => log info s"Actor已经关闭: ${ref.path}"
      context.system terminate
  }
}

object WechatApp
{
  final val NAME = "wechat-app"
  @inline
  final def props(implicit factory: ActorFactory) = Props(new WechatApp)
}