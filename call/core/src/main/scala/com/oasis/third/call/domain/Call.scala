package com.oasis.third.call.domain

import java.util.Date

import com.oasis.third.call.domain.event.{Bound, HungUp, Updated}
import com.oasis.third.call.protocol.CallRequest.Bind
import org.ryze.micro.core.domain.DomainError
import org.ryze.micro.core.tool.Regex
import org.ryze.micro.protocol.tool.ProtobufTool

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
  `type`     : Option[String] =  None,
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
  callTime   : Option[Long]   = None,
  createTime : Date,
  updateTime : Option[Date]   = None
)
{
  //挂断
  @inline
  def hangUp(e: HungUp) = copy (updateTime = e.updateTime map ProtobufTool.toDate)
  //更新
  @inline
  def update(e: Updated) = copy (
    callId      = e.callId,
    call        = e.call,
    to          = e.to,
    `type`      = e.`type`,
    ringTime    = e.ringTime map ProtobufTool.toDate,
    beginTime   = e.beginTime map ProtobufTool.toDate,
    endTime     = e.endTime map ProtobufTool.toDate,
    status      = e.status,
    eventStatus = e.eventStatus,
    updateTime  = e.updateTime map ProtobufTool.toDate
  )
}

object Call
{
  import reactivemongo.bson.Macros

  implicit val bson = Macros.handler[Call]

  @inline
  private[this] def validateCall(call: String) = Regex.MOBILE findFirstIn call map (Right(_)) getOrElse Left(DomainError(0, "呼叫方错误!"))
  @inline
  private[this] def validateTo(to: String) = Regex.MOBILE findFirstIn to map (Right(_)) getOrElse Left(DomainError(1, "被呼叫方错误!"))

  /**
    * 绑定请求校验
    */
  @inline
  final def validate(r: Bind) = for
  {
    _ <- validateCall(r.call)
    _ <- validateTo(r.to)
  } yield r
  /**
    * 创建
    */
  @inline
  final def create(e: Bound) = Call(
    _id        = e.id,
    call       = e.call,
    to         = e.to,
    noticeUri  = e.noticeUri,
    thirdId    = e.thirdId,
    createTime = (e.createTime map ProtobufTool.toDate) get
  )
}
