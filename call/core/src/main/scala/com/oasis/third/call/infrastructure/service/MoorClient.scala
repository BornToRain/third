package com.oasis.third.call.infrastructure.service

import akka.actor.Props
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity, Uri}
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern._
import com.oasis.third.call.infrastructure.service.MoorClient.request.HangUp
import io.circe.Json
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}
import org.ryze.micro.core.http.JsonSupport
import org.ryze.micro.core.tool.{Base64, ConfigLoader, DateTool, MD5}

import scala.concurrent.Future

/**
  * 容联七陌客户端
  */
class MoorClient(implicit runtime: ActorRuntime) extends ActorL with JsonSupport
{
  import runtime._

  /**
    * 容联七陌鉴权 请求头部分
    * Base64编码(账户id+冒号+时间戳)
    */
  @inline
  private[this] def authHeader(timeStamp: String) = Base64 encode s"${MoorClient.account }:$timeStamp"
  /**
    * 容联七陌鉴权 请求参数部分
    * MD5编码(帐号id+帐号APISecret+时间戳)
    * 转大写
    */
  @inline
  private[this] def authParameter(timeStamp: String) = MD5 encode s"${MoorClient.account}${MoorClient.secret}$timeStamp"
  /**
    * 容联七陌客户端POST
    * 已拼接好鉴权部分
    */
  @inline
  private[this] def post(uri: String, entity: Future[RequestEntity]) = for
  {
    a <- entity
    b <-
    {
      val ts = DateTool.datetimeStamp
      Future(authHeader(ts), authParameter(ts))
    }
    c <- Http() singleRequest HttpRequest(HttpMethods.POST, Uri(s"${MoorClient.gateway}$uri") withQuery Query("sig" -> b._2), entity = a)
    d <- Unmarshal(c.entity).to[Json]
  } yield d
  //挂断
  @inline
  private[this] def hangUp(c: HangUp) = post(s"v20160818/call/hangup/${MoorClient.account}", Marshal(c).to[RequestEntity]) map (_.as[String] getOrElse "")

  override def receive =
  {
    //挂断
    case c: HangUp => hangUp(c) pipeTo sender
  }
}

object MoorClient extends ConfigLoader
{
  final val NAME = "moor-client"

  private[this] val moorConfig = loader getConfig "7moor"

  @inline
  final def props(implicit runtime: ActorRuntime) = Props(new MoorClient)

  lazy val account = moorConfig getString "account"
  lazy val secret  = moorConfig getString "secret"
  lazy val gateway = "http://apis.7moor.com/"

  object request
  {
    //挂断
    case class HangUp(CallId: Option[String], Agent: Option[String], ActionID: String)
  }
}
