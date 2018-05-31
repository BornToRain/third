package com.oasis.third.pa.infrastructure.service

import akka.actor.Props
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern._
import com.oasis.third.pa.infrastructure.service.OasisClient.{request, response}
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}
import org.ryze.micro.core.http.JsonSupport
import org.ryze.micro.core.tool.ConfigLoader

import scala.concurrent.Future

/**
  * 泓华客户端
  */
case class OasisClient(implicit runtime: ActorRuntime) extends ActorL with JsonSupport
{
  import runtime._

  private[this] def post(uri: String)(entity: Future[RequestEntity]) = for
  {
    a <-
    {
      println(s"gateway: ${OasisClient.gateway}$uri")
      entity
    }
    b <- Http().singleRequest(HttpRequest(HttpMethods.POST, Uri(s"${OasisClient.gateway}$uri"), entity = a))
  } yield b
  //openId注册泓华账号
  private[this] def registerBy(openId: String) = for
  {
    a <- post("/userAccount/third/register")(Marshal(Map("openId" -> openId)).to[RequestEntity])
    b <- Unmarshal(a).to[response.Register]
  } yield b

  override def receive =
  {
    case request.Register(openId) => registerBy(openId) pipeTo sender
  }
}

object OasisClient extends ConfigLoader
{
  final val NAME = "oasis-client"

  private[this] val oasisConfig = loader.getConfig("oasis")

  def props(implicit runtime: ActorRuntime) = Props(new OasisClient)

  //泓华网关地址
  lazy val gateway = oasisConfig.getString("gateway")

  object request
  {
    case class Register(openId: String)
  }

  object response
  {
    case class Register(accountId: String, openId: String)
  }
}
