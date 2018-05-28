package com.oasis.third.sms

import akka.actor.{Props, Terminated}
import akka.contrib.persistence.mongodb.MongoReadJournal
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.scaladsl.EventsByTagQuery
import com.oasis.third.sms.application.service.SmsService
import com.oasis.third.sms.domain.SmsAggregate
import com.oasis.third.sms.infrastructure.SmsRepositoryImpl
import com.oasis.third.sms.infrastructure.service.AlibabaClient
import com.oasis.third.sms.interfaces.api.v1.SmsApi
import com.typesafe.config.ConfigFactory
import org.ryze.micro.core.actor.{ActorFactory, ActorL}
import org.ryze.micro.core.mongodb.MongoDBClient
import org.ryze.micro.core.tool.ConfigLoader

import scala.language.postfixOps

/**
  * 短信模块启动
  */
object SmsAppStartUp extends App with ConfigLoader
{
  implicit val factory = ActorFactory(loader)

  factory.system actorOf(SmsApp.props, SmsApp.NAME)
}

class SmsApp(implicit factory: ActorFactory) extends ActorL
{
  import factory.runtime

  private[this] val readJournal = PersistenceQuery(factory.system).readJournalFor[EventsByTagQuery](MongoReadJournal.Identifier)
  private[this] val mongodb     = MongoDBClient(factory.config).db
  private[this] val repository  = SmsRepositoryImpl(mongodb)
  private[this] val alibaba     = new AlibabaClient
  private[this] val domain      = context actorOf(SmsAggregate props, SmsAggregate.NAME)
  private[this] val service     = context actorOf(SmsService props (domain, alibaba), SmsService.NAME)
  private[this] val api         = context actorOf(SmsApi props service, SmsApi.NAME)

  domain :: service :: api :: Nil foreach (context watch)

  override def receive =
  {
    case Terminated(ref) => log.info(s"Actor已经关闭: ${ref.path}")
      context.system.terminate
  }
}

object SmsApp
{
  final val NAME = "sms-app"

  def props(implicit factory: ActorFactory) = Props(new SmsApp)
}
