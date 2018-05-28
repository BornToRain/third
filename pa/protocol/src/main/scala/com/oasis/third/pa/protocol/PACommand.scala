package com.oasis.third.pa.protocol

import com.oasis.third.pa.domain.event.Paid
import com.oasis.third.pa.domain.vo.{OrderDetail => OD}
import com.oasis.third.pa.protocol.PAValueObject.OrderDetail
import org.ryze.micro.protocol.domain.DomainCommand

/**
  * 平安金管家领域命令
  */
sealed trait PACommand extends DomainCommand
{
  val id   : String
  val event: PAEvent
}

object PACommand
{
  case class Pay(id: String, orderId: String, openId: String, amount: Int, payType: String, details: Seq[OrderDetail],
    detailUri: String) extends PACommand
  {
    val xs = details map (d => OD(d.id, d.name, d.count, d.uri.orNull, d.imageUri.orNull, d.subject.orNull, d.amount))
    override val event = Paid(id, orderId, openId, amount, payType, xs, detailUri)
  }
}
