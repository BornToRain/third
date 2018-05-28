package com.oasis.third.call.domain

import akka.actor.Props
import com.oasis.third.call.domain.event.{Bound, HungUp}
import com.oasis.third.call.protocol.{CallCommand, CallEvent}
import com.oasis.third.call.protocol.CallCommand.{Bind, HangUp, Update}
import org.ryze.micro.core.actor.ActorRuntime
import org.ryze.micro.core.domain.AggregateRoot

class CallAggregate extends AggregateRoot[Option[Call], CallCommand, CallEvent]
{
  override var state: Option[Call] = _

  override def updateState(event: CallEvent): Unit = event match
  {
    case e: Bound  => state = Some(Call(e.id, call = e.call, to = e.to, noticeUri = e.noticeUri, thirdId = e.thirdId))
    case e: HungUp => state = state
    case e: Update => state = state map (_ copy(callId = e.callId, `type` = e.`type`, ringTime = e.ringTime, beginTime = e.beginTime, endTime = e
      .endTime, status = e.status, eventStatus = e.eventStatus))
  }
  override def receiveCommand =
  {
    case c: Bind   => persist(c.event)(afterPersist)
    case c: HangUp => persist(c.event)(afterPersist)
    case c: Update => persist(c.event)(afterPersist)
  }
  override def persistenceId = CallAggregate.NAME
}

object CallAggregate
{
  final val NAME = "call-aggregate"

  def props(implicit runtime: ActorRuntime) = Props(new CallAggregate)
}
