package com.oasis.third.wechat.infrastructure.service

import akka.actor.Props
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern._
import com.oasis.third.wechat.infrastructure.service.PaymentClient.Request
import com.oasis.third.wechat.infrastructure.service.PaymentClient.response.{JS, Response}
import com.oasis.third.wechat.infrastructure.service.WechatClient.response.JsSDK
import com.oasis.third.wechat.infrastructure.tool.XMLTool
import com.oasis.third.wechat.infrastructure.tool.CommonTool
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}
import org.ryze.micro.core.tool.{DateTool, MD5}

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Random

class PaymentClient(implicit runtime: ActorRuntime) extends ActorL
{
  import runtime._

  @inline
  private[this] def post(uri: String, xml: String) = for
  {
    a <- Http() singleRequest HttpRequest(HttpMethods.POST, Uri(s"${PaymentClient.gateway}$uri"), entity = HttpEntity(xml))
    b <- Unmarshal(a.entity).to[String]
  } yield b
  /**
    * 填充商户号等基础数据
    */
  @inline
  private[this] def assembly(r: Request) = r copy(
    appid      = Some(r.appid getOrElse WechatClient.appId),
    mch_id     = Some(r.mch_id getOrElse WechatClient.mchId),
    total_fee  = r.total_fee * 100,
    nonce_str  = CommonTool createRandom 32,
    notify_url = Some(PaymentClient.notify_Uri)
  )

  import io.circe.syntax._
  import io.circe.generic.auto._

  /**
    * 生成签名
    * 1.排序
    * 2.key=value&
    * 3.key
    * 4.MD5加密
    * 5.转大写
    */
  @inline
  private[this] def createSign(r: Seq[(String, Any)]) =
    Option(MD5 encode (("" /: r.sortBy(_._1))((_, t) => s"${t._1}=${t._2}") + s"key=${WechatClient.key}") toUpperCase)
  //付款
  @inline
  private[this] def payment(r: Request) =
  {
    val request  = assembly(r)
    val data     = request copy (sign = createSign(request.asJsonObject.toList))
    val xml      = XMLTool toXML data
    log info s"发起微信支付: $xml"
    val response = post("/pay/unifiedorder", xml) map XMLTool.fromXML[Response]
    log info s"微信支付返回: $response"

    r.trade_type match
    {
      case "JSAPI" => response map
      {
        d =>
          val js = JS(r.appid getOrElse WechatClient.appId, CommonTool createRandom 32,
            DateTool.timeStamp, s"prepay_id=${d.prepay_id get}", "MD5")
          js copy (paySign = createSign(js.asJsonObject.toList))
      }
    }
  }

  override def receive =
  {
    case r: Request => payment(r) pipeTo sender
  }
}

object PaymentClient
{
  final val NAME = "payment-client"
  @inline
  final def props(implicit runtime: ActorRuntime) = Props(new PaymentClient)

  lazy val gateway    = "https://api.mch.weixin.qq.com"
  lazy val notify_Uri = "https://buztest190.oasisapp.cn/honghclient/servlet/WeChatTrade"

  case class Request
  (
    appid           : Option[String],
    mch_id          : Option[String],
    device_info     : Option[String] = Some("WEB"),
    nonce_str       : String = CommonTool createRandom 32,
    sign            : Option[String] = None,
    sign_type       : Option[String] = Some("MD5"),
    body            : String,
    detail          : Option[String] = None,
    attach          : Option[String] = None,
    out_trade_no    : String,
    fee_type        : Option[String] = Some("CNY"),
    total_fee       : BigDecimal,
    spbill_create_ip: Option[String] = None,
    notify_url      : Option[String] = None,
    trade_type      : String,
    //JSAPI方式
    openid          : Option[String] = None,
    //NATIVE方式
    product_id      : Option[String] = None
  )

  object response
  {
    case class Response
    (
      return_code : String,
      return_msg  : String,
      appid       : String,
      mch_id      : String,
      nonce_str   : String,
      sign        : String,
      result_code : String,

      code_url    : Option[String],
      trade_type  : Option[String],
      prepay_id   : Option[String]
    )
    case class JS(appId: String, nonceStr: String, timeStamp: Long, `package`: String, signType: String, paySign: Option[String] = None)
  }
}