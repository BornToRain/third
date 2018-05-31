package com.oasis.third.wechat.interfaces.api.v1

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.pattern._
import com.oasis.third.wechat.infrastructure.service.WechatClient.request._
import com.oasis.third.wechat.infrastructure.service.WechatClient.response.JsSDK
import com.oasis.third.wechat.infrastructure.tool.XMLTool
import com.oasis.third.wechat.interfaces.dto.{Text, WechatDTO}
import io.circe.JsonObject
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}
import org.ryze.micro.core.http.RestApi
import org.ryze.micro.core.tool.{ConfigLoader, DateTool}

import scala.util.{Failure, Random, Success}
import scala.xml.XML

class WechatApi(client: ActorRef)(implicit runtime: ActorRuntime) extends RestApi with ActorL
{
  import runtime._

  Http(runtime.as).bindAndHandle(route, WechatApi.host, WechatApi.port) onComplete
  {
    case Success(d) => log.info(s"微信模块启动成功: ${d.localAddress}")
    case Failure(e) => log.error(s"微信模块启动失败: ${e.getMessage}")
  }

  //产生指定位数随机数
  def createRandom(i: Int) = Random.alphanumeric.take(i).mkString

  override def route = logRequestResult(("wechat", Logging.InfoLevel))
  {
    pathPrefix("v1" / "wechat")
    {
      pathEnd
      {
        //校验微信服务器
        (get & parameters('signature, 'timestamp, 'nonce, 'echostr))
        {
          (a, b, c, d) => onSuccess((client ? Check(a, b, c)).mapTo[Option[Boolean]])
          {
            //微信必须要字符串不是JSON字符串 即content非"content"
            case Some(true) => complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, d))
            case _          => complete("")
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
        r => complete
        {
          (client ? GetOAuth2(r)).mapTo[JsonObject] map ("openid" -> _("openid"))
        }
      } ~
      //获取js-sdk
      (path("js-sdk") & parameter('uri))
      {
        r => complete
        {
          (client ? GetJsSign(DateTool.datetimeStamp, createRandom(32), r)).mapTo[JsSDK]
        }
      }
    }
  }
  override def receive = Actor.emptyBehavior
}

object WechatApi extends ConfigLoader
{
  private[this] val httpConfig = loader getConfig "http"

  final val NAME = "wechat-api"

  def props(client: ActorRef)(implicit runtime: ActorRuntime) = Props(new WechatApi(client))

  lazy val host = httpConfig getString "host"
  lazy val port = httpConfig getInt    "port"
}