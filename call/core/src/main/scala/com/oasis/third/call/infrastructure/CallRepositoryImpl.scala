package com.oasis.third.call.infrastructure

import com.oasis.third.call.domain.{Call, CallRepository}
import org.ryze.micro.core.actor.ActorRuntime
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument

case class CallRepositoryImpl(db: DefaultDB)(implicit runtime: ActorRuntime) extends CallRepository
{
  import runtime._

  private[this] val collection = db[BSONCollection]("call")

  @inline
  private[this] def byId(id: String) = BSONDocument("_id" -> id)

  override def insert(d: Call) = collection insert d map (_.ok)
  override def selectOne(id: String) = collection.find(byId(id)).one[Call]
  override def update(d: Call) = collection update(byId(d._id), d) map (_.ok)
}
