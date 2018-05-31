package org.ryze.micro.core.tool

/**
  * 正则
  */
object Regex
{
  //手机号
  lazy val MOBILE = """(13\d|14[579]|15[^4\D]|17[^49\D]|18\d)\d{8}""".r
  //URI
  lazy val URI    = "[a-zA-z]+://[^\\s]*".r
}