package com.oasis.third.wechat.infrastructure.tool

import scala.util.Random

object CommonTool
{
  //产生指定位数随机数
  @inline
  final def createRandom(i: Int) = Random.alphanumeric.take(i).mkString
}
