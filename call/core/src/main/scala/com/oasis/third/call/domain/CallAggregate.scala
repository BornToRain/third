package com.oasis.third.call.domain

import akka.actor.Props
import akka.cluster.Cluster
import com.oasis.third.call.domain.event.{Bound, HungUp, Updated}
import com.oasis.third.call.protocol.{CallCommand, CallEvent}
import com.oasis.third.call.protocol.CallCommand.{Bind, HangUp, Update}
import org.ryze.micro.core.actor.ActorRuntime
import org.ryze.micro.core.domain.AggregateRoot

class CallAggregate extends AggregateRoot[Domain, CallCommand, CallEvent]
{
  override var state: Domain = _

  val cluster = Cluster(context.system)

  override def updateState(event: CallEvent): Unit = event match
  {
    case e: Bound   => state = Some(Call create e)
    case e: HungUp  => state = state map (_ hangUp e)
    case e: Updated => state = state map (_ update e)
  }
  override def receiveCommand =
  {
    case c: Bind   => log.info(s"Result on ${cluster.selfAddress.hostPort} is: ${state}")
      persist(c.event)(afterPersist)
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
