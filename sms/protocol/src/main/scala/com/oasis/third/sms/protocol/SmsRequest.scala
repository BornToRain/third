package com.oasis.third.sms.protocol

/**
  * 短信请求
  */
sealed trait SmsRequest

object SmsRequest
{
  case class Create(mobile: String, `type`: String) extends SmsRequest
  case class Validate(mobile: String, `type`: String, captcha: String) extends SmsRequest
}