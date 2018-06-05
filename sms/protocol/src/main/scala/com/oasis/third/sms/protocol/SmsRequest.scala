package com.oasis.third.sms.protocol

/**
  * 短信请求
  */
sealed trait SmsRequest

object SmsRequest
{
  case class Create(mobile: String, smsType: String) extends SmsRequest
  case class Validate(mobile: String, smsType: String, captcha: String) extends SmsRequest
}