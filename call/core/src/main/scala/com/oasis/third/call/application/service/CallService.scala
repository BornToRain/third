package com.oasis.third.call.application.service

import akka.actor.{ActorRef, Props}
import akka.pattern._
import com.oasis.third.call.domain._
import com.oasis.third.call.infrastructure.service.MoorClient
import com.oasis.third.call.infrastructure.service.MoorClient.HangUpRequest
import com.oasis.third.call.protocol.CallRequest
import com.oasis.third.call.protocol.CallCommand.{Bind, HangUp, Update}
import com.oasis.third.call.protocol.CallState.GetStateBy
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}
import org.ryze.micro.core.domain.DomainError
import org.ryze.micro.core.tool.{DateTool, IdWorker}

import scala.concurrent.Future

class CallService
(
  domain: ActorRef,
  read  : ActorRef,
  moor  : MoorClient
)(implicit runtime: ActorRuntime) extends ActorL
{
  import runtime._

  //绑定
  private[this] def bind(r: CallRequest.Bind) = Call validate r map
  {
    _ =>
      val id = IdWorker.getId
      domain ! Bind(id, r.call, r.to, r.noticeUri, r.thirdId)
      id
  }
  //挂断
  private[this] def hangUp(cmd: HangUp) = for
  {
    a <- (domain ? cmd).mapTo[Option[Call]]
    b <- a match
    {
      case Some(d) => moor hangUp HangUpRequest(d.callId, None, d._id) map (Right(_))
      case _       => Future(Left(DomainError.NotFound))
    }
  } yield b
  //获取绑定电话
  @inline
  private[this] def getBindMobile(c: GetStateBy) = (read ? c).mapTo[Option[String]]
  //更新通话
  private[this] def update(cmd: Update) =
  {
    //本次通话时长 单位:秒
    val callTime = for
    {
      a <- cmd.beginTime
      b <- cmd.endTime
    } yield DateTool.compare(a)(b)(DateTool.SECONDS)
    domain ! (cmd copy (callTime = callTime))
  }

  override def receive =
  {
    case r: CallRequest.Bind => sender ! bind(r)
    case c: HangUp           => hangUp(c) pipeTo sender
    case c: GetStateBy       => getBindMobile(c) pipeTo sender
    case c: Update           => update(c)
  }
}

object CallService
{
  final val NAME = "call-service"

  def props(domain: ActorRef, read: ActorRef, moor: MoorClient)(implicit runtime: ActorRuntime) =
    Props(new CallService(domain, read, moor))
}