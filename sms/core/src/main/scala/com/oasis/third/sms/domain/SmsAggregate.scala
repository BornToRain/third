package com.oasis.third.sms.domain

import akka.actor.Props
import com.oasis.third.sms.domain.event.Created
import com.oasis.third.sms.protocol.{SmsCommand, SmsEvent}
import com.oasis.third.sms.protocol.SmsCommand.Create
import org.ryze.micro.core.domain.AggregateRoot

class SmsAggregate extends AggregateRoot[Domain, SmsCommand, SmsEvent]
{
  override var state: Domain = _
  override def updateState(event: SmsEvent): Unit = event match
  {
    case e: Created => state = Some(Sms create e)
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
  @inline
  final def props = Props(new SmsAggregate)
}