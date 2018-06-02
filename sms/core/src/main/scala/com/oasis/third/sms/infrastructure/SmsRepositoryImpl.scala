package com.oasis.third.sms.infrastructure

import com.oasis.third.sms.domain.{Sms, SmsRepository}
import org.ryze.micro.core.actor.ActorRuntime
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.Future

case class SmsRepositoryImpl(db: Future[DefaultDB])(implicit runtime: ActorRuntime) extends SmsRepository
{
  import runtime._

  @inline
  private[this] def collection: Future[BSONCollection] = db map (_ collection "sms")

  override def insert(d: Sms) = collection flatMap (_ insert d map (_.ok))
}
