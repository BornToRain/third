package com.oasis.third.wechat.infrastructure.service

import akka.actor.Props
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.pattern._
import cats.data.EitherT
import cats.instances.future._
import com.oasis.third.wechat.infrastructure.service.PaymentClient.Request
import com.oasis.third.wechat.infrastructure.service.PaymentClient.response.{JS, Response}
import com.oasis.third.wechat.infrastructure.tool.{CommonTool, XMLTool}
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}
import org.ryze.micro.core.domain.DomainError
import org.ryze.micro.core.tool.{DateTool, MD5}

import scala.concurrent.Future
import scala.language.postfixOps

class PaymentClient(implicit runtime: ActorRuntime) extends ActorL
{
  import runtime._

  @inline
  private[this] def post(uri: String, xml: String) = for
  {
    a <- Http() singleRequest HttpRequest(HttpMethods.POST, Uri(s"${PaymentClient.gateway}$uri"), entity = HttpEntity(xml))
    b <- a.entity.dataBytes.runFold(Response.empty)((_, s) => XMLTool.fromXML(s.utf8String))
    c <- Future
    {
      if (b.return_code == "FAIL")Left(DomainError(0, b.return_msg))
      else Right(b)
    }
  } yield c
  /**
    * 填充商户号等基础数据
    */
  @inline
  private[this] def assembly(r: Request) = r copy(
    mch_id      = Some(r.mch_id getOrElse WechatClient.mchId),
    device_info = Some("web"),
    sign_type   = Some("MD5"),
    fee_type    = Some("CNY"),
    total_fee   = (BigDecimal(r.total_fee) * 100).toInt.toString,
    nonce_str   = CommonTool createRandom 32,
    notify_url  = Some(WechatClient.notify_Uri)
  )

  /**
    * 生成签名
    * 1.排序
    * 2.key=value&secretKey
    * 4.MD5加密
    * 5.转大写
    */
  @inline
  private[this] def createSign(r: Seq[(String, Json)]) =
  {
    val step1 = r filterNot(t => t._1 == "sign" || t._1 == "paySign") sortBy(_._1)
    val step2 = ("" /: step1)((s, t) => s + s"${t._1}=${t._2.asString getOrElse ""}&") + s"key=${WechatClient.key}"
    Some(MD5 encode step2 toUpperCase)
  }
  //付款
  @inline
  private[this] def payment(r: Request) =
  {
    val request  = assembly(r)
    val data     = request copy (sign = createSign(request.asJsonObject.toList))
    val xml      = XMLTool toXML data
    log info s"发起微信支付: $xml"
    val response = EitherT(post("pay/unifiedorder", xml))
    response map
    {
      d => d.trade_type match
      {
        case Some("JSAPI") =>
          val js = JS(
            appId     = r.appid getOrElse WechatClient.appId,
            nonceStr  = CommonTool createRandom 32,
            timeStamp = DateTool.timeStamp.toString,
            `package` = s"prepay_id=${d.prepay_id get}",
            signType  = "MD5"
          )
          js copy (paySign = createSign(js.asJsonObject.toList))
      }
    } value
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

  lazy val gateway    = "https://api.mch.weixin.qq.com/"

  case class Request
  (
    appid           : Option[String],
    mch_id          : Option[String],
    device_info     : Option[String] = Some("WEB"),
    nonce_str       : String = CommonTool createRandom 32,
    sign            : Option[String] = None,
    sign_type       : Option[String] = Some("MD5"),
    body            : String,
    attach          : Option[String] = None,
    out_trade_no    : String,
    fee_type        : Option[String] = Some("CNY"),
    total_fee       : String,
    spbill_create_ip: Option[String] = None,
    notify_url      : Option[String] = None,
    trade_type      : String,
    //JSAPI方式
    openid          : Option[String] = None
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
      err_code    : String,
      err_code_des: String,
      device_info : String,

      code_url    : Option[String] = None,
      trade_type  : Option[String] = None,
      prepay_id   : Option[String] = None
    )
    object Response
    {
      @inline
      final def empty = Response("", "", "", "", "", "", "", "", "", "")
    }

    case class JS(appId: String, nonceStr: String, timeStamp: String, `package`: String, signType: String, paySign: Option[String] = None)
  }
}