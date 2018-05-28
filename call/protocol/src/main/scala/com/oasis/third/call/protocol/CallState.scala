package com.oasis.third.call.protocol

/**
  * 通话状态
  */
sealed trait CallState

object CallState
{
  case class GetStateBy(call: String) extends CallState
}