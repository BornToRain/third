package com.oasis.third.sms.domain

import akka.actor.Props
import com.oasis.third.sms.domain.event.Created
import com.oasis.third.sms.protocol.{SmsCommand, SmsEvent}
import com.oasis.third.sms.protocol.SmsCommand.Create
import org.ryze.micro.core.domain.AggregateRoot
import org.ryze.micro.protocol.tool.ProtobufTool

class SmsAggregate extends AggregateRoot[Option[Sms], SmsCommand, SmsEvent]
{
  override var state: Option[Sms] = _
  override def updateState(event: SmsEvent): Unit = event match
  {
    case e: Created => state = Some(Sms(e.id, e.mobile, e.`type`, e.captcha, e.messageId, (e.createTime map ProtobufTool.toDate).get))
  }
  override def receiveCommand =
  {
    case c: Create => persist(c.event)(afterPersist)
  }
  override def persistenceId = SmsAggregate.NAME
}

object SmsAggregate
{
  final val NAME = "sms-aggregate"

  def props = Props(new SmsAggregate)
}