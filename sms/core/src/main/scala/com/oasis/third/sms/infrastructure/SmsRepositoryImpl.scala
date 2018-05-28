package com.oasis.third.sms.infrastructure

import com.oasis.third.sms.domain.{Sms, SmsRepository}
import org.ryze.micro.core.actor.ActorRuntime
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection

case class SmsRepositoryImpl(db: DefaultDB)(implicit runtime: ActorRuntime) extends SmsRepository
{
  import runtime._

  private[this] val collection = db[BSONCollection]("sms")

  override def insert(d: Sms) = collection insert d map (_.ok)
}
