package com.oasis.third.pa.infrastructure.service

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.oasis.third.pa.infrastructure.service.OasisClient.Response.Register
import org.ryze.micro.core.actor.ActorRuntime
import org.ryze.micro.core.http.JsonSupport
import org.ryze.micro.core.tool.ConfigLoader

import scala.concurrent.Future

/**
  * 泓华客户端
  */
case class OasisClient(implicit runtime: ActorRuntime) extends JsonSupport
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

  /**
    * openId注册泓华账号
    */
  def registerBy(openId: String) = for
  {
    a <- post("/userAccount/third/register")(Marshal(Map("openId" -> openId)).to[RequestEntity])
    b <- Unmarshal(a).to[Register]
  } yield b
}

object OasisClient extends ConfigLoader
{
  private[this] val oasisConfig = loader.getConfig("oasis")

  //泓华网关地址
  lazy val gateway = oasisConfig.getString("gateway")

  object Response
  {
    //注册用户返回
    case class Register(accountId: String, openId: String)
  }
}
