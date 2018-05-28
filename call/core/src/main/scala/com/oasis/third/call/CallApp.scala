package com.oasis.third.call

import akka.actor.{Props, Terminated}
import akka.contrib.persistence.mongodb.MongoReadJournal
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.scaladsl.EventsByTagQuery
import com.oasis.third.call.CallAppStartUp.loader
import com.oasis.third.call.application.service.CallService
import com.oasis.third.call.domain.{CallAggregate, CallReadSide}
import com.oasis.third.call.infrastructure.CallRepositoryImpl
import com.oasis.third.call.infrastructure.service.MoorClient
import com.oasis.third.call.interfaces.api.v1.CallApi
import org.ryze.micro.core.actor.{ActorFactory, ActorL}
import org.ryze.micro.core.mongodb.MongoDBClient
import org.ryze.micro.core.redis.RdsClient
import org.ryze.micro.core.tool.ConfigLoader

import scala.language.postfixOps

object CallAppStartUp extends App with ConfigLoader
{
  implicit val factory = ActorFactory(loader)

  factory.system actorOf(CallApp.props, CallApp.NAME)
}

/**
  * 电话模块
  */
class CallApp(implicit factory: ActorFactory) extends ActorL
{
  import factory.runtime

  private[this] val readJournal = PersistenceQuery(factory.system).readJournalFor[EventsByTagQuery](MongoReadJournal.Identifier)
  private[this] val redis       = RdsClient(factory.config).rds
  private[this] val mongodb     = MongoDBClient(factory.config).db
  private[this] val moor        = MoorClient()
  private[this] val repository  = CallRepositoryImpl(mongodb)
  private[this] val domain      = context actorOf(CallAggregate.props, CallAggregate.NAME)
  private[this] val read        = context actorOf(CallReadSide.props(readJournal, repository, redis), CallReadSide.NAME)
  private[this] val service     = context actorOf(CallService.props(domain, read, moor), CallService.NAME)
  private[this] val api         = context actorOf(CallApi.props(service), CallApi.NAME)

  domain :: read :: service :: api :: Nil foreach (context watch)

  override def receive =
  {
    case Terminated(ref) => log.info(s"Actor已经关闭: ${ref.path}")
    context.system.terminate
  }
}

object CallApp
{
  final val NAME = "call-app"

  def props(implicit factory: ActorFactory) = Props(new CallApp)
}
