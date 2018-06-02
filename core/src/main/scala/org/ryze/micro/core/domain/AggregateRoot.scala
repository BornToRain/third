package org.ryze.micro.core.domain

import akka.actor.ActorLogging
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.persistence.{PersistentActor, SnapshotOffer}
import org.ryze.micro.protocol.domain.{DomainCommand, DomainEvent}

import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
  * 聚合根
  */
abstract class AggregateRoot[State: ClassTag, Command <: DomainCommand: ClassTag, Event <: DomainEvent: ClassTag] extends PersistentActor with ActorLogging
{
  private[this] var eventCount = 0

  @inline
  private[this] def reply(): Unit = sender ! state
  @inline
  private[this] def publish(event: Event): Unit = mediator ! Publish(persistenceId, event, sendOneMessageToEachGroup = true)

  var state: State
  //分布式订阅
  val mediator = DistributedPubSub(context.system).mediator

  //更新状态
  def updateState(event: Event): Unit
  /**
    * 1.计算事件
    * 2.保存快照
    * 3.更新状态
    * 4.回复
    * 5.发布事件到事件总线
    */
  @inline
  def afterPersist(event: Event): Unit =
  {
    eventCount += 1
    if(eventCount == AggregateRoot.SNAPSHOT)
    {
      log debug "保存快照"
      saveSnapshot(state)
      eventCount = 0
    }
    updateState(event)
    reply()
    publish(event)
  }

  //1分钟钝化
  override def preStart(): Unit = context setReceiveTimeout 1.minutes
  override def receiveRecover =
  {
    case SnapshotOffer(_, s: State) => state = s
    case event: Event               => eventCount +=1
      updateState(event)
  }
}

object AggregateRoot
{
  //10事件 => 1快照
  final val SNAPSHOT = 10
}
