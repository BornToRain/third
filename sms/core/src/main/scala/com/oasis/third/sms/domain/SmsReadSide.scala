package com.oasis.third.sms.domain

import akka.actor.Props
import akka.persistence.query.Offset
import akka.persistence.query.scaladsl.EventsByTagQuery
import akka.stream.scaladsl.Sink
import com.oasis.third.sms.domain.event.Created
import com.oasis.third.sms.protocol.SmsEvent
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}

class SmsReadSide
(
  readJournal: EventsByTagQuery,
  repository : SmsRepository
)(implicit runtime: ActorRuntime) extends ActorL
{
  import runtime._

  //根据事件标签获取实时事件流
  readJournal.eventsByTag(SmsEvent.TAG, Offset.noOffset) map (_.event) runWith Sink.actorRef(self, "completed")

  override def receive =
  {
    case e: Created => repository insert (Sms create e)
  }
}

object SmsReadSide
{
  final val NAME = "sms-read"

  def props(readJournal: EventsByTagQuery, repository: SmsRepository)(implicit runtime: ActorRuntime) =
    Props(new SmsReadSide(readJournal, repository))
}