package com.oasis.third.call.protocol

import java.util.Date

import com.oasis.third.call.domain.event.{Bound, HungUp, Updated}
import org.ryze.micro.protocol.domain.DomainCommand
import org.ryze.micro.protocol.tool.ProtobufTool

/**
  * 通话领域命令
  */
sealed trait CallCommand extends DomainCommand
{
  val id: String
  val event: CallEvent
}

object CallCommand
{
  //绑定
  case class Bind(id: String, call: String, to: String, noticeUri: Option[String], thirdId: Option[String]) extends CallCommand
  {
    override val event = Bound(id, call, to, noticeUri, thirdId)
  }
  //挂断
  case class HangUp(id: String) extends CallCommand
  {
    override val event = HungUp(id)
  }
  //更新
  case class Update(id: String, call: String, to: String, `type`: Option[String], ringTime: Option[Date], beginTime: Option[Date],
    endTime: Option[Date], status: Option[String], eventStatus: Option[String], recordFile: Option[String], fileServer: Option[String],
    callId: Option[String], callTime: Option[Long] = None) extends CallCommand
  {
    override val event = Updated(id, call, to, `type`, ringTime map ProtobufTool.toTimestamp, beginTime map ProtobufTool.toTimestamp,
      endTime map ProtobufTool.toTimestamp, status, eventStatus, recordFile, fileServer, callId)
  }
}
