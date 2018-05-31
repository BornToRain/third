package com.oasis.third.sms.domain

import java.util.Date

import com.oasis.third.sms.domain.event.Created
import com.oasis.third.sms.protocol.SmsRequest.{Create, Validate}
import org.ryze.micro.core.domain.DomainError
import org.ryze.micro.core.tool.Regex
import org.ryze.micro.protocol.tool.ProtobufTool

case class Sms
(
  _id       : String,
  //手机号
  mobile    : String,
  //短信类型
  `type`    : String,
  //验证码
  captcha   : Option[String] = None,
  //阿里消息ID
  messageId : String,
  createTime: Date
)

object Sms
{
  import reactivemongo.bson.Macros

  implicit val bson = Macros.handler[Sms]

  private[this] def validateMobile(moblie: String) = Regex.MOBILE.findFirstIn(moblie) map (Right(_)) getOrElse Left(DomainError(0, "手机号错误!"))
  private[this] def validateType(`type`: String) = Option(`type`) match
  {
    case Some(d) if SmsType.seq map (_.name) contains d => Right(d)
    case _                                              => Left(DomainError(1, "短信类型错误!"))
  }
  private[this] def validateCaptcha(captcha: Option[String]) = captcha match
  {
    case a@ Some(d) if d.length == 4 => Right(a)
    case _                           => Left(DomainError(2, "验证码错误!"))
  }

  /**
    * 创建请求校验
    */
  def validate(r: Create) = for
  {
    _ <- validateMobile(r.mobile)
    _ <- validateType(r.`type`)
  } yield r
  /**
    * 校验请求校验
    */
  def validate(r: Validate) = for
  {
    _ <- validateMobile(r.mobile)
    _ <- validateType(r.`type`)
    _ <- validateCaptcha(Some(r.captcha))
  } yield r
  /**
    * 创建
    */
  def create(e: Created) = Sms(e.id, e.mobile, e.`type`, e.captcha, e.messageId, (e.createTime map ProtobufTool.toDate).get)
}