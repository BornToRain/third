package com.oasis.third.sms.protocol

import org.ryze.micro.protocol.domain.DomainEvent

/**
  * 短信领域事件
  */
trait SmsEvent extends DomainEvent

object SmsEvent
{
  final val TAG = "sms-tag"
}