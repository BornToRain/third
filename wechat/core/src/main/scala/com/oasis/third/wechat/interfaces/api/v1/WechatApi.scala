package com.oasis.third.wechat.interfaces.api.v1

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.model.StatusCodes._
import akka.pattern._
import com.oasis.third.wechat.Result
import com.oasis.third.wechat.infrastructure.service.WechatClient
import com.oasis.third.wechat.infrastructure.service.PaymentClient.Request
import com.oasis.third.wechat.infrastructure.service.PaymentClient.response.JS
import com.oasis.third.wechat.infrastructure.service.WechatClient.request._
import com.oasis.third.wechat.infrastructure.service.WechatClient.response.JsSDK
import com.oasis.third.wechat.infrastructure.tool.{CommonTool, XMLTool}
import com.oasis.third.wechat.interfaces.dto.{Text, WechatDTO}
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}
import org.ryze.micro.core.http.RestApi
import org.ryze.micro.core.tool.{ConfigLoader, DateTool}

import scala.util.{Failure, Success}
import scala.xml.XML

class WechatApi
(
  client : ActorRef,
  payment: ActorRef
)(implicit runtime: ActorRuntime) extends RestApi with ActorL
{
  import runtime._

  Http(runtime.as) bindAndHandle(route, WechatApi.host, WechatApi.port) onComplete
  {
    case Success(d) => log info s"微信模块启动成功: ${d.localAddress}"
    case Failure(e) => log error s"微信模块启动失败: ${e.getMessage}"
  }

  override def route = logRequestResult(("wechat", Logging.InfoLevel))
  {
    pathPrefix("v1" / "wechat")
    {
      pathEnd
      {
        //校验微信服务器
        (get & parameters('signature, 'timestamp, 'nonce, 'echostr))
        {
          (a, b, c, d) => onSuccess((client ? Check(a, b, c)).mapTo[Boolean])
          {
            //微信必须要字符串不是JSON字符串 即content非"content"
            case true => complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, d))
            case _    => complete("")
          }
        } ~
        //处理微信请求
        (post & extractDataBytes)
        {
          r => complete
          {
            r.runFold("")((_, s) => s.utf8String) map
            {
              d =>
                val wechat = WechatDTO(XML loadString d)
                log info s"处理微信请求: $wechat"
                //目前统一回复文本消息
                val result = Text(wechat.FromUserName, wechat.ToUserName, wechat.CreateTime, MsgId = wechat.MsgId, Content = "泓华医疗,让健康更简单!")
                XMLTool toXML result
            }
          }
        }
      } ~
      //code获取OAuth2
      (path("oauth2") & parameter('code))
      {
        r => onSuccess((client ? GetOAuth2(r)).mapTo[Result[Map[String, String]]])
        {
          case Right(d) => complete(d)
          case Left(e)  => complete(BadRequest -> e)
        }
      } ~
      //获取js-sdk
      path("js")
      {
        (get & parameter('uri))
        {
          r => complete
          {
            (client ? GetJsSign(DateTool.datetimeStamp, CommonTool createRandom 32, r)).mapTo[JsSDK]
          }
        } ~
        //公众号支付
        (post & entity(as[Request]))
        {
          r =>
            val request = r copy (appid = Some(WechatClient.appId))
            onSuccess((payment ? request).mapTo[Result[JS]])
            {
              case Right(d) => complete(d)
              case Left(e)  => complete(BadRequest -> e)
            }
        }
      } ~
      //小程序支付
      (path("applet") & post & entity(as[Request]))
      {
        r =>
          val request = r copy (appid = Some(WechatClient.appletId))
          onSuccess((payment ? request).mapTo[Result[JS]])
          {
            case Right(d) => complete(d)
            case Left(e)  => complete(BadRequest -> e)
          }
      }
    }
  }
  override def receive = Actor.emptyBehavior
}

object WechatApi extends ConfigLoader
{
  final val NAME = "wechat-api"
  @inline
  final def props(client: ActorRef, payment: ActorRef)(implicit runtime: ActorRuntime) = Props(new WechatApi(client, payment))

  private[this] val httpConfig = loader getConfig "http"


  lazy val host = httpConfig getString "host"
  lazy val port = httpConfig getInt    "port"
}