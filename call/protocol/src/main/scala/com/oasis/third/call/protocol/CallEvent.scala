package com.oasis.third.call.protocol

import org.ryze.micro.protocol.domain.DomainEvent

/**
  * 通话领域事件 protobuf序列化
  */
trait CallEvent extends DomainEvent

object CallEvent
{
  final val TAG = "call-tag"
}