package com.oasis.third.call.application.service

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.pattern._
import cats.data.OptionT
import cats.instances.future._
import com.oasis.third.call.domain._
import com.oasis.third.call.infrastructure.service.MoorClient.request
import com.oasis.third.call.interfaces.assembler.CallAssembler
import com.oasis.third.call.protocol.CallRequest
import com.oasis.third.call.protocol.CallCommand.{Bind, HangUp, Update}
import com.oasis.third.call.protocol.CallState.GetStateBy
import io.circe.syntax._
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}
import org.ryze.micro.core.domain.DomainError
import org.ryze.micro.core.http.JsonSupport
import org.ryze.micro.core.tool.{DateTool, IdWorker}

import scala.concurrent.Future
import scala.language.postfixOps

class CallService
(
  domain: ActorRef,
  read  : ActorRef,
  moor  : ActorRef
)(implicit runtime: ActorRuntime) extends ActorL with JsonSupport
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
      //调用容联七陌客户端挂断
      case Some(d) => (moor ? request.HangUp(d.callId, None, d._id)).mapTo[String] map (Right(_))
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
    OptionT((domain ? (cmd copy (callTime = callTime))).mapTo[Domain]) map
    {
      //回调地址存在则回调
      d => if(d.noticeUri.isDefined)
      {
        val entity = HttpEntity(ContentTypes.`application/json`, printer pretty (CallAssembler toDTO d).asJson)
        Http().singleRequest(HttpRequest(HttpMethods.POST, uri = d.noticeUri.get, entity = entity))
      }
    }
  }

  override def receive =
  {
    //绑定
    case r: CallRequest.Bind => sender ! bind(r)
    //挂断
    case c: HangUp           => hangUp(c) pipeTo sender
    //获取绑定电话
    case c: GetStateBy       => getBindMobile(c) pipeTo sender
    //更新通话
    case c: Update           => update(c)
  }
}

object CallService
{

  final val NAME = "call-service"

  def props(domain: ActorRef, read: ActorRef, moor: ActorRef)(implicit runtime: ActorRuntime) =
    Props(new CallService(domain, read, moor))

  case class CallDTO
  (
    id       : String,
    //第三方ID
    thirdId  : Option[String],
    //接听状态
    callState: Option[String],
    //通话状态
    state    : Option[String],
    //通话时长
    callTime : Option[Long],
    beginTime: Option[String],
    endTime  : Option[String]
  )

}
