package com.oasis.third.pa.protocol

import org.ryze.micro.protocol.domain.DomainEvent

/**
  * 平安金管家领域事件 protobuf序列化
  */
trait PAEvent extends DomainEvent

object PAEvent
{
  final val TAG = "pa-tag"
}