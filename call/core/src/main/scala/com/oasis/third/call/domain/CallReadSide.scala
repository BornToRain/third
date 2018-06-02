package com.oasis.third.call.domain

import akka.actor.Props
import akka.pattern._
import akka.persistence.query.Offset
import akka.persistence.query.scaladsl.EventsByTagQuery
import akka.stream.scaladsl.Sink
import cats.data.OptionT
import cats.instances.future._
import com.oasis.third.call.domain.event.{Bound, Updated}
import com.oasis.third.call.protocol.CallEvent
import com.oasis.third.call.protocol.CallState.GetStateBy
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}
import redis.RedisClient

import scala.concurrent.duration._
import scala.language.postfixOps

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
  readJournal eventsByTag (CallEvent.TAG, Offset.noOffset) map (_.event) runWith Sink.actorRef(self, "completed")

  override def receive =
  {
    case e: Bound         =>
      //存储30分钟
      redis set (s"$key_binding${e.call}", e.to, Some(30.minutes toSeconds))
      repository insert (Call create e)
    case e: Updated       => OptionT(repository selectOne e.id) map (_ update e)
    case GetStateBy(call) => redis.get[String](s"$key_binding$call") pipeTo sender
  }
}

object CallReadSide
{
  final val NAME = "call-read"

  @inline
  final def props(readJournal: EventsByTagQuery, repository: CallRepository, redis: RedisClient)(implicit runtime: ActorRuntime) =
    Props(new CallReadSide(readJournal, repository, redis))
}
