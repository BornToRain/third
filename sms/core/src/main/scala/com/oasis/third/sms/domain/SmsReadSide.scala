package com.oasis.third.sms.domain

import akka.actor.Props
import akka.persistence.query.Offset
import akka.persistence.query.scaladsl.EventsByTagQuery
import akka.stream.scaladsl.Sink
import com.oasis.third.sms.domain.event.Created
import com.oasis.third.sms.protocol.SmsEvent
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}
import org.ryze.micro.protocol.tool.ProtobufTool

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
    case e: Created => repository insert Sms(e.id, e.mobile, e.`type`, e.captcha, e.messageId, (e.createTime map ProtobufTool.toDate).get)
  }
}

object SmsReadSide
{
  final val NAME = "sms-read"

  def props(readJournal: EventsByTagQuery, repository: SmsRepository)(implicit runtime: ActorRuntime) =
    Props(new SmsReadSide(readJournal, repository))
}