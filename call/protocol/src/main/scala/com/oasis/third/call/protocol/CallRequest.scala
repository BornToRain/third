package com.oasis.third.call.protocol

/**
  * 通话请求
  */
sealed trait CallRequest

object CallRequest
{
  case class Bind(call: String, to: String, noticeUri: Option[String], thirdId: Option[String]) extends CallRequest
}