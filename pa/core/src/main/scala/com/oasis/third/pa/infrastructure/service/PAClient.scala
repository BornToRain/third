package com.oasis.third.pa.infrastructure.service

import java.net.URLEncoder
import java.time.LocalDateTime

import akka.actor.Props
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern._
import com.oasis.third.pa.infrastructure.service.PAClient.OrderDetail
import com.oasis.third.pa.infrastructure.service.PAClient.Request.GetAccessToken
import com.oasis.third.pa.protocol.PARequest.Pay
import com.paic.palife.common.util.encry.la.LASecurityUtils
import io.circe.JsonObject
import io.circe.syntax._
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}
import org.ryze.micro.core.http.JsonSupport
import org.ryze.micro.core.tool.{ConfigLoader, DateTool}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * 平安金管家客户端
  */
case class PAClient(implicit runtime: ActorRuntime) extends ActorL with JsonSupport
{
  import runtime._

  var accessToken: String = _

  /**
    * 1秒延迟后开始定时
    * 每19天刷新1次
    */
  context.system.scheduler.schedule(1.seconds, 19.days)
  {
    getAccessToken
  }

  @inline
  private[this] def convert(response: HttpResponse) = Unmarshal(response.entity)
  //生成签名
  @inline
  private[this] def sign(map: Map[String, String]) =
    URLEncoder.encode(LASecurityUtils.createSignatureForParameters(PAClient.privateKey, map.asJava), HttpCharsets.`UTF-8`.value)
  private[this] def get(uri: String)(qs: (String, String)*) = for
  {
    a <- Http().singleRequest(HttpRequest(uri = Uri(uri).withQuery(Query(qs: _*))))
    b <- convert(a).to[JsonObject]
  } yield b
  private[this] def post(uri: String)(entity: Map[String, String]) = for
  {
    a <- Future
    {
      val e= HttpEntity(ContentType.apply(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`),
        //一定要记得最后那个&不要去掉，平安那狗第三方就这么要求的。
        entity.toList.sorted map (t => s"${t._1}=${t._2}&") reduce (_+_))
      log.info(s"$e")
      e
    }
    b <- Http().singleRequest(HttpRequest(HttpMethods.POST, Uri(s"${PAClient.gateway}$uri"), entity = a)
    .withHeaders(RawHeader("Accept", "application/json")))
    c <- convert(b).to[JsonObject] map (_("data"))
  } yield c
  //转换支付类型
  private[this] def convert(payType: String) = payType match
  {
    //微信
    case "3803" => "03"
    //支付宝
    case "3804" => "02"
    case _      => "暂不支持"
  }
  //网络请求获取金管家令牌
  private[this] def getAccessToken = get(PAClient.oauth2)("client_id" -> PAClient.clientId, "client_secret" -> PAClient.secret,
        "grant_type" -> "client_credentials") map
  {
    d =>
      val ac = d("data") map (_.hcursor.get[String]("access_token") getOrElse "") getOrElse ""
      log info s"获取金管家AccessToken成功: $ac"
      accessToken = ac
  } recover
  {
    case e: Throwable => log error s"获取金管家AccessToken失败: ${e.getMessage}"
  }
  //订单回传
  private[this] def upload(r: Pay) =
  {
    //当前时间
    val now     = LocalDateTime.now
    //订单失效时间 15分钟
    val expire  = now plusMinutes 15L
    //订单详情
    val details = r.details map (d => OrderDetail(d.name, d.count.toString, d.id, d.imageUri, commoditySubject = d.subject getOrElse "null",
      commodityPrice = d.amount.toString))
    // 订单回传参数
    val params  = Map (
      //商户号
      "merchantCode"          -> PAClient.merchantCode,
      //商户号ID
      "merchantId"            -> PAClient.merchantId,
      //订单号
      "merOrderNo"            -> r.orderId,
      //交易类型
      "tradeType"             -> "JSAPI",
      //订单金额(单位:分)
      "orderAmount"           -> r.amount.toString,
      //货币类型
      "currency"              -> "CNY",
      //下单时间yyyy-MM-dd HH:mm:ss
      "orderPrepayTime"       -> DateTool.format(now),
      //支付时间yyyy-MM-dd HH:mm:ss
      "orderPayTime"          -> DateTool.format(now),
      //不能填0、不能不传、不能填null,然而屁用没有。
      "couponAmount"          -> "1",
      "couponId"              -> null,
      //这个单词一定要打错，平安那边只认这个。一群XX写的代码
      "counponIds"            -> null,
      //支付类型 02 => 支付宝 03 => 微信 04 => 万里通 05 => 快钱 06 => 优惠券 07 => 预付卡 08 => 手机话费，09 => 虚拟币
      "payType"               -> convert(r.payType),
      //订单状态 02 => 已付款
      "orderStatus"           -> "02",
      //订单详情Uri
      "detailUrl"             -> r.detailUri,
      //订单实付金额(单位:) 可为0,不能带小数
      "realAmount"            -> r.amount.toString,
      //订单类型 00 => 虚拟类 01 => 实物类
      "orderCategory"         -> "00",
      //订单详情
      "orderDetail"           -> details.asJson.noSpaces,
      //唯一标识
      "openId"                -> r.openId,
      //下单失效时间yyyy-MM-dd HH:mm:ss
      "orderPrepayExpireTime" -> DateTool.format(expire),
      //产品编码
      "productCode"           -> "VLRT",
      "agentNo"               -> null,
      "waybillId"             -> null,
      "orderSubject"          -> null
    )
    val request = params + ("securitySign" -> sign(params))

    post(s"/order/postBack/access_token=$accessToken")(request)
  }

  override def receive =
  {
    //获取金管家AccessToken
    case GetAccessToken => sender ! accessToken
    //订单回传
    case r: Pay         => upload(r) pipeTo sender
  }
}

object PAClient extends ConfigLoader
{
  final val NAME = "pa-client"

  private[this] val paConfig = loader.getConfig("pa")

  def props(implicit runtime: ActorRuntime) = Props(new PAClient)

  //平安公钥
  lazy val publicKey    = paConfig.getString("public-key")
  //私钥
  lazy val privateKey   = paConfig.getString("private-key")
  //商户号
  lazy val merchantCode = paConfig.getString("merchant-code")
  //商户号ID
  lazy val merchantId   = paConfig.getString("merchant-id")
  //平安网关地址
  lazy val gateway      = paConfig.getString("gateway")
  //平安OAuth2地址
  lazy val oauth2       = paConfig.getString("oauth2")
  lazy val clientId     = paConfig.getString("client-id")
  lazy val secret       = paConfig.getString("secret")

  object Request
  {
    case object GetAccessToken
  }

  //商品
  case class OrderDetail
  (
    //名称
    commodity        : String,
    //数量
    commodityCount   : String,
    //编码
    commodityID      : String,
    //图片URI
    commodityImageUrl: Option[String],
    //URI
    commodityUrl     : String = "null",
    //摘要
    commoditySubject : String,
    //价格(单位:分)
    commodityPrice   : String,
  )
}