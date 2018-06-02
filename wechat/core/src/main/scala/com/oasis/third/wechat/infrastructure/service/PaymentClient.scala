package com.oasis.third.wechat.infrastructure.service

import akka.actor.Props
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern._
import com.oasis.third.wechat.infrastructure.service.PaymentClient.request.Payment
import com.oasis.third.wechat.infrastructure.tool.XMLTool
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}
import org.ryze.micro.core.tool.MD5

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Random

class PaymentClient(implicit runtime: ActorRuntime) extends ActorL
{
  import runtime._

  @inline
  private[this] def post(uri: String, entity: Future[RequestEntity]) = for
  {
    a <- entity
    b <- Http() singleRequest HttpRequest(HttpMethods.POST, Uri(s"${PaymentClient.gateway}$uri"), entity = a)
    c <- Unmarshal(b.entity).to[String]
  } yield c
  /**
    * 填充商户号等基础数据
    */
  @inline
  private[this] def assembly(r: Payment) = r copy(
    appid      = Some(r.appid getOrElse WechatClient.appId),
    mch_id     = Some(r.mch_id getOrElse WechatClient.mchId),
    total_fee  = r.total_fee * 100,
    nonce_str  = PaymentClient createRandom 32,
    notify_url = Some(PaymentClient.notify_Uri)
  )
  import io.circe.syntax._
  /**
    * 生成签名
    * 1.排序
    * 2.key=value&
    * 3.key
    * 4.MD5加密
    * 5.转大写
    */
  @inline
  private[this] def createSign(r: Payment) =
    MD5.encode(("" /: r.asJsonObject.toList.sorted) ((_, t) => s"${t._1}=${t._2.asString.get}&") + s"key=${WechatClient.key}").toUpperCase

  //    (MD5 encode (("" /: r.asJsonObject.toList.sorted)((_, t) => s"${t._1}=${t._2.asString.get}&") + s"key=${WechatClient.key}")) toUpperCase
  @inline
  private[this] def payment(r: Payment) =
  {
    val data = assembly(r)
    data copy (sign = Some(createSign(data)))
    val xml  = XMLTool.toXML(data)
    log info s"发起微信支付: $xml"

    post("/pay/unifiedorder", Marshal(r).to[RequestEntity])
  }

  override def receive =
  {
    case r: Payment => payment(r) pipeTo sender
  }
}

object PaymentClient
{
  final val NAME = "payment-client"
  @inline
  final def props(implicit runtime: ActorRuntime) = Props(new PaymentClient)

  //产生指定位数随机数
  @inline
  private[PaymentClient] def createRandom(i: Int) = Random.alphanumeric.take(i).mkString

  lazy val gateway    = "https://api.mch.weixin.qq.com"
  lazy val notify_Uri = "https://buztest190.oasisapp.cn/honghclient/servlet/WeChatTrade"


  object request
  {
    case class Payment
    (
      appid           : Option[String],
      mch_id          : Option[String],
      device_info     : Option[String] = Some("WEB"),
      nonce_str       : String = createRandom(32),
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
      trade_type      : Option[String] = None,
      //JSAPI方式
      openid          : Option[String] = None,
      //NATIVE方式
      product_id      : Option[String] = None
    )
  }
}