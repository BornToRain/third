package com.oasis.third.wechat.infrastructure.service

import akka.actor.Props
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern._
import io.circe.JsonObject
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}
import org.ryze.micro.core.http.JsonSupport
import org.ryze.micro.core.tool.{ConfigLoader, SHA1}

import scala.concurrent.Future
import scala.concurrent.duration._


/**
  * 微信客户端
  */
class WechatClient(implicit runtime: ActorRuntime) extends ActorL with JsonSupport
{
  import WechatClient._
  import WechatClient.request._
  import WechatClient.response._
  import runtime._

  private[this] val kv = "access_token" -> "#"

  var accessToken: String = _
  var jsApiTicket: String = _

  /**
    * 1秒延迟后开始定时
    * 每1小时刷新1次
    */
  context.system.scheduler.schedule(1.seconds, 1.hours)
  {
    getAccessToken map (_ => getJsApiTicket)
  }

  //替换AccessToken
  private[this] def replaceAccessToken(qs: (String, String)*) = Future
  {
    if (qs contains (("access_token", "#"))) (Seq.empty[(String, String)] /: qs)
    {
      case (xs, (x@"access_token", _)) => xs :+ (x, accessToken)
      case (xs, t)                     => xs :+ t
    }
    else qs
  }
  @inline
  private[this] def convert(response: HttpResponse) = Unmarshal(response.entity)
  private[this] def get(uri: String)(qs: (String, String)*) = for
  {
    a <- replaceAccessToken(qs: _*)
    b <- Http().singleRequest(HttpRequest(uri = Uri(s"$gateway$uri") withQuery Query(a: _*)))
    c <- convert(b).to[JsonObject]
  } yield c
  private[this] def post(uri: String)(entity: Future[RequestEntity])(qs: (String, String)*) = for
  {
    a <- replaceAccessToken(qs: _*)
    b <- entity
    c <- Http().singleRequest(HttpRequest(HttpMethods.POST, Uri(s"$gateway$uri") withQuery Query(a: _*), entity = b))
    d <- convert(c).to[JsonObject]
  } yield d
  /**
    * 校验微信服务器签名
    * 1.字典序排序
    * 2.拼接
    * 3.SHA-1加密
    * 4.对比
    */
  @inline
  private[this] def check(signature: String)(timestamp: String)(nonce: String) =
    Option(SHA1 encode ("" /: Seq(token, timestamp, nonce).sorted)(_ + _) equalsIgnoreCase signature)
  /**
    * JS-SDK签名
    * 1.排序
    * 2.拼接
    * 3.SHA-1加密
    */
  @inline
  private[this] def jsSign(timeStamp: String)(nonceStr: String)(uri: String) =
    SHA1 encode s"jsapi_ticket=$jsApiTicket&noncestr=$nonceStr&timestamp=$timeStamp&url=$uri"
  //网络请求获取AccessToken
  private[this] def getAccessToken = get("cgi-bin/token")("grant_type" -> "client_credential",
    "appid" -> appId, "secret" -> secret) map
  {
    d => val ac = d("access_token") map(_.asString.get) getOrElse ""
      log info s"获取微信AccessToken成功: $ac"
      accessToken = ac
  } recover
  {
    case e: Throwable => log error s"获取微信AccessToken失败: ${e.getMessage}"
  }
  //网络请求获取JsApiTicket
  private[this] def getJsApiTicket = get("cgi-bin/ticket/getticket")("access_token" -> "#", "type" -> "jsapi") map
  {
    d => val ticket = d("ticket") map(_.asString.get) getOrElse ""
      log info s"获取微信JsApiTicket成功: $ticket"
      jsApiTicket = ticket
  } recover
  {
    case e: Throwable => log error s"获取微信JsApiTicket失败: ${e.getMessage}"
  }
  @inline
  private[this] def getOAuth2(code: String) = get("sns/oauth2/access_token")("appid" -> WechatClient.appId,
    "secret" -> WechatClient.secret, "code" -> code, "grant_type" -> "authorization_code")

  override def receive =
  {
    //获取AccessToken
    case GetAccessToken          => sender ! accessToken
    //获取JsApiTicket
    case GetJsApiTicket          => sender ! jsApiTicket
    //code换取OAuth2
    case GetOAuth2(code: String) => getOAuth2(code) pipeTo sender
    //校验微信服务器签名
    case Check(a, b, c)          => sender ! check(a)(b)(c)
    //获取Js-sdk签名
    case GetJsSign(a, b, c)      => val sign = jsSign(a)(b)(c)
      sender ! JsSDK(appId, a, b, sign)
  }
}

object WechatClient extends ConfigLoader
{
  final val NAME = "wechat-client"

  def props(implicit runtime: ActorRuntime) = Props(new WechatClient)

  private[this] val wechatConfig = loader getConfig "wechat"

  lazy val appId   = wechatConfig getString "app-id"
  lazy val mchId   = wechatConfig getString "mch-id"
  lazy val key     = wechatConfig getString "key"
  lazy val secret  = wechatConfig getString "secret"
  lazy val token   = wechatConfig getString "token"
  lazy val gateway = "https://api.weixin.qq.com/"

  object request
  {
    case object GetAccessToken
    case object GetJsApiTicket
    case class GetOAuth2(code: String)
    case class Check(signature: String, timestamp: String, nonce: String)
    case class GetJsSign(timeStamp: String, nonceStr: String, uri: String)
  }

  object response
  {
    case class JsSDK(appId: String, timestamp: String, nonceStr: String, signature: String)
  }
}