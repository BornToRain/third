package com.oasis.third.sms.application.service

import akka.actor.{ActorRef, Props}
import com.oasis.third.sms.domain.{Sms, SmsType}
import com.oasis.third.sms.domain.SmsType.{Notice, Payment}
import com.oasis.third.sms.infrastructure.service.AlibabaClient
import com.oasis.third.sms.infrastructure.tool.TotpCaptcha
import com.oasis.third.sms.protocol.SmsCommand
import com.oasis.third.sms.protocol.SmsRequest.{Create, Validate}
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}
import org.ryze.micro.core.tool.IdWorker

class SmsService(domain: ActorRef, alibaba: AlibabaClient)(implicit runtime: ActorRuntime) extends ActorL
{
  //创建短信
  private[this] def create(r: Create) = Sms validate r map
  {
    _ =>
      val id       = IdWorker.getId
      val (c, mId) = SmsType(r.`type`) match
      {
        //达人通知
        case Notice  => (None, alibaba notice r.mobile)
        //其他类型(需要验证码的)
        case d       => val c = Some(TotpCaptcha.getCaptcha(r.mobile)(r.`type`))
          val id = d match
          {
            case Payment => alibaba.payment(r.mobile)(c.get)
            case _       => alibaba.captcha(r.mobile)(c.get)
          }
          (c, id)
      }
      domain ! SmsCommand.Create(id, r.mobile, r.`type`, c, mId)
      id
  }
  //校验短信
  private[this] def validate(r: Validate) = Sms validate r map
  {
    _ => TotpCaptcha.getCaptcha(r.mobile)(r.`type`) == r.captcha
  }

  override def receive =
  {
    case r: Create   => sender ! create(r)
    case r: Validate => sender ! validate(r)
  }
}

object SmsService
{
  final val NAME = "sms-service"

  def props(domain: ActorRef, alibaba: AlibabaClient)(implicit runtime: ActorRuntime) = Props(new SmsService(domain, alibaba))
}