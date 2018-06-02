package com.oasis.third.sms.application.service

import akka.actor.{ActorRef, Props}
import akka.pattern._
import com.oasis.third.sms.domain.{Sms, SmsType}
import com.oasis.third.sms.infrastructure.service.AlibabaClient.request.Send
import com.oasis.third.sms.infrastructure.tool.TotpCaptcha
import com.oasis.third.sms.protocol.SmsCommand
import com.oasis.third.sms.protocol.SmsRequest.{Create, Validate}
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}
import org.ryze.micro.core.tool.IdWorker

import scala.concurrent.Future


class SmsService(domain: ActorRef, alibaba: ActorRef)(implicit runtime: ActorRuntime) extends ActorL
{
  import runtime._

  //创建短信
  @inline
  private[this] def create(r: Create) = Sms validate r match
  {
    case Right(_) =>
      val id   = IdWorker.getId
      val code = Some(TotpCaptcha getCaptcha (r.mobile, r.`type`))
      (alibaba ? Send(r.mobile, SmsType(r.`type`), code)).mapTo[String] map
      {
        mId =>
          domain ! SmsCommand.Create(id, r.mobile, r.`type`, code, mId)
          Right(id)
      }
    case Left(e) => Future(Left(e))
  }
  //校验短信
  @inline
  private[this] def validate(r: Validate) = Sms validate r map ((TotpCaptcha getCaptcha (r.mobile, r.`type`)) == _.captcha)

  override def receive =
  {
    case r: Create   => create(r) pipeTo sender
    case r: Validate => sender ! validate(r)
  }
}

object SmsService
{
  final val NAME = "sms-service"
  @inline
  final def props(domain: ActorRef, alibaba: ActorRef)(implicit runtime: ActorRuntime) = Props(new SmsService(domain, alibaba))
}