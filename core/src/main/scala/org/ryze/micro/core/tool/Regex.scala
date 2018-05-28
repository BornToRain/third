package org.ryze.micro.core.tool

/**
  * 正则
  */
object Regex
{
  //手机号
  lazy val MOBILE = """1(([3,5,8]\d{9})|(4[5,7]\d{8})|(7[0,6-8]\d{8}))""".r
  //URI
  lazy val URI    = "http(s)?://([\\\\w-]+\\\\.)+[\\\\w-]+(/[\\\\w- ./?%&=]*)?".r
}