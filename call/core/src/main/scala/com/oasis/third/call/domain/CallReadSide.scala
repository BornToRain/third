package com.oasis.third.call.domain

import akka.actor.Props
import akka.persistence.query.Offset
import akka.persistence.query.scaladsl.EventsByTagQuery
import akka.stream.scaladsl.Sink
import com.oasis.third.call.domain.event.{Bound, Updated}
import com.oasis.third.call.protocol.CallEvent
import com.oasis.third.call.protocol.CallState.GetStateBy
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}
import org.ryze.micro.protocol.tool.ProtobufTool
import redis.RedisClient

import scala.concurrent.duration._

class CallReadSide
(
  readJournal: EventsByTagQuery,
  repository : CallRepository,
  redis      : RedisClient
)(implicit runtime: ActorRuntime)
extends ActorL
{
  import runtime._

  //根据事件标签获取实时事件流
  readJournal.eventsByTag(CallEvent.TAG, Offset.noOffset) map (_.event) runWith Sink.actorRef(self, "completed")

  override def receive =
  {
    case e: Bound         =>
      //存储30分钟
      redis.set(s"$key_binding${e.call}", e.to, Some(30.minutes.toSeconds))
      repository insert Call(_id = e.id, call = e.call, to = e.to, noticeUri = e.noticeUri, thirdId = e.thirdId)
    case e: Updated       => repository update Call(
      _id         = e.id,
      callId      = e.callId,
      call        = e.call,
      to          = e.to,
      `type`      = e.`type`,
      ringTime    = e.ringTime map ProtobufTool.toDate,
      beginTime   = e.beginTime map ProtobufTool.toDate,
      endTime     = e.endTime map ProtobufTool.toDate,
      status      = e.status,
      eventStatus = e.eventStatus,
      callTime    = e.callTime
    )
    case GetStateBy(call) => redis.get[String](s"$key_binding$call")
  }
}

object CallReadSide
{
  final val NAME = "call-read"

  def props(readJournal: EventsByTagQuery, repository: CallRepository, redis: RedisClient)(implicit runtime: ActorRuntime) =
    Props(new CallReadSide(readJournal, repository, redis))
}
