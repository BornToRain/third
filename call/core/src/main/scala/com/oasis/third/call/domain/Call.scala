package com.oasis.third.call.domain

import java.util.Date

import com.oasis.third.call.protocol.CallRequest.Bind
import org.ryze.micro.core.domain.DomainError
import org.ryze.micro.core.tool.Regex

import scala.language.postfixOps

case class Call
(
  _id        : String,
  //容联七陌唯一标识
  callId     : Option[String] = None,
  //呼叫方
  call       : String,
  //被呼叫方
  to         : String,
  //通话类型
  `type`     : Option[String] = None,
  //通话振铃时间
  ringTime   : Option[Date]   = None,
  //接通时间
  beginTime  : Option[Date]   = None,
  //结束事件
  endTime    : Option[Date]   = None,
  //接听状态
  status     : Option[String] = None,
  //事件状态
  eventStatus: Option[String] = None,
  //第三方唯一标识
  thirdId    : Option[String] = None,
  //回调通知地址
  noticeUri  : Option[String] = None,
  //通话时长
  callTime   : Option[Long]   = None
)

object Call
{
  import reactivemongo.bson.Macros

  implicit val bson = Macros.handler[Call]

  private[this] def validateCall(call: String) = Regex.MOBILE findFirstIn call map (Right(_)) getOrElse Left(DomainError(0, "呼叫方错误!"))
  private[this] def validateTo(to: String) = Regex.MOBILE findFirstIn to map (Right(_)) getOrElse Left(DomainError(1, "被呼叫方错误!"))
  private[this] def validateNoticeUri(noticeUri: Option[String]) = noticeUri match
  {
    case s if s.exists(Regex.URI.pattern matcher _ matches) => Right(s)
    case _                                                  => Left(DomainError(2, "回调通知地址错误!"))
  }
  private[this] def validateThirdId(thirdId: Option[String]) = thirdId match
  {
    case s @ Some(_) => Right(s)
    case _           => Left(DomainError(3, "第三方唯一标识为空!"))
  }

  /**
    * 绑定请求校验
    */
  def validate(r: Bind) = for
  {
    _ <- validateCall(r.call)
    _ <- validateTo(r.to)
    _ <- validateNoticeUri(r.noticeUri)
    _ <- validateThirdId(r.thirdId)
  } yield r
}
