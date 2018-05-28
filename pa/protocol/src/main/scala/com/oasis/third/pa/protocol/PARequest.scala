package com.oasis.third.pa.protocol

import com.oasis.third.pa.protocol.PAValueObject.OrderDetail

/**
  * 平安金管家请求
  */
sealed trait PARequest

object PARequest
{
  //付款
  case class Pay(orderId: String, openId: String, amount: Int, payType: String, details: Seq[OrderDetail], detailUri: String) extends PARequest
}