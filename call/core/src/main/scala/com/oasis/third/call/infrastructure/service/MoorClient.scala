package com.oasis.third.call.infrastructure.service

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity, Uri}
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.oasis.third.call.infrastructure.service.MoorClient.HangUpRequest
import io.circe.JsonObject
import org.ryze.micro.core.actor.ActorRuntime
import org.ryze.micro.core.http.JsonSupport
import org.ryze.micro.core.tool.{Base64, ConfigLoader, DateTool, MD5}

import scala.concurrent.Future

/**
  * 容联七陌客户端
  */
case class MoorClient(implicit runtime: ActorRuntime) extends JsonSupport
{
  import runtime._

  /**
    * 容联七陌鉴权 请求头部分
    * Base64编码(账户id+冒号+时间戳)
    */
  @inline
  private[this] def authHeader(timeStamp: String) = Base64.encode(s"${MoorClient.account }:$timeStamp")
  /**
    * 容联七陌鉴权 请求参数部分
    * MD5编码(帐号id+帐号APISecret+时间戳)
    * 转大写
    */
  @inline
  private[this] def authParameter(timeStamp: String) = MD5.encode(s"${MoorClient.account}${MoorClient.secret}$timeStamp")
  /**
    * 容联七陌客户端POST
    * 已拼接好鉴权部分
    */
  private[this] def post(uri: String)(entity: Future[RequestEntity]) = for
  {
    a <- entity
    b <-
    {
      val ts = DateTool.datetimeStamp
      Future(authHeader(ts), authParameter(ts))
    }
    c <- Http().singleRequest(HttpRequest(HttpMethods.POST, Uri(s"${MoorClient.gateway}$uri")
    .withQuery(Query("sig" -> b._2)), entity = a))
    d <- Unmarshal(c.entity).to[JsonObject]
  } yield d

  /**
    * 电话挂断
    */
  @inline
  def hangUp(r: HangUpRequest) = post(s"v20160818/call/hangup/${MoorClient.account}")(Marshal(r).to[RequestEntity]) map (_.toString)
}

object MoorClient extends ConfigLoader
{
  private[this] val moorConfig = loader.getConfig("7moor")

  lazy val account = moorConfig.getString("account")
  lazy val secret  = moorConfig.getString("secret")
  lazy val gateway = "http://apis.7moor.com/"

  //挂断请求
  case class HangUpRequest(CallId: Option[String], Agent: Option[String], ActionID: String)
}
