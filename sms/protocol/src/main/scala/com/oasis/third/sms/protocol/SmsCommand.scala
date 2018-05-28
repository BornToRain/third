package com.oasis.third.sms.protocol

import java.util.Date

import com.oasis.third.sms.domain.event.Created
import org.ryze.micro.protocol.domain.DomainCommand
import org.ryze.micro.protocol.tool.ProtobufTool

/**
  * 短信领域命令
  */
sealed trait SmsCommand extends DomainCommand
{
  val id   : String
  val event: SmsEvent
}

object SmsCommand
{
  case class Create(id: String, mobile: String, `type`: String, captcha: Option[String], messageId: String,
    createTime: Date = new Date) extends SmsCommand
  {
    override val event = Created(id, mobile, `type`, captcha, messageId, Some(ProtobufTool toTimestamp createTime))
  }
}