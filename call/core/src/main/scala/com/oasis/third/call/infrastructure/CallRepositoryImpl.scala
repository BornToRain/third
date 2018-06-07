package com.oasis.third.call.infrastructure

import com.oasis.third.call.domain.{Call, CallRepository}
import org.ryze.micro.core.actor.ActorRuntime
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument

import scala.concurrent.Future

case class CallRepositoryImpl(db: Future[DefaultDB])(implicit runtime: ActorRuntime) extends CallRepository
{
  import runtime._

  @inline
  private[this] def collection: Future[BSONCollection] = db map (_ collection "call")
  @inline
  private[this] def byId(id: String) = BSONDocument("_id" -> id)

  override def insert(d: Call) = collection flatMap (_ insert d map (_.ok))
  override def selectOne(id: String) = collection flatMap (_.find (byId(id)).one[Call])
  override def update(d: Call) = collection flatMap (_ update (byId(d._id), d) map (_.ok))
}
