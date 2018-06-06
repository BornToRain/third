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

  @inline
  private[this] def getStateBy(call: String) = OptionT(redis.get[String](s"$KEY_BINDING$call")) map (_ split "|") value

  override def receive =
  {
    case e: Bound         =>
      //懒得写解析,直接字符串|分割,第零个是id,第一个是被呼叫方.
      //存储30分钟
      redis set (s"$KEY_BINDING${e.call}", s"${e.id}|${e.to}", Some(30.minutes toSeconds))
      repository insert (Call create e)
    case e: Updated       =>
      //删除绑定关系
      redis del s"$KEY_BINDING${e.call}"
      repository selectOne e.id map
      {
        case Some(d) => d update e
        case _       =>
      }
    case GetStateBy(call) => getStateBy(call) pipeTo sender
  }
}

object CallReadSide
{
  final val NAME = "call-read"

  @inline
  final def props(readJournal: EventsByTagQuery, repository: CallRepository, redis: RedisClient)(implicit runtime: ActorRuntime) =
    Props(new CallReadSide(readJournal, repository, redis))

}
