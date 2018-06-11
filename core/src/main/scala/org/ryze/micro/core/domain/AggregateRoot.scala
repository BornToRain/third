package org.ryze.micro.core.domain

import akka.actor.ActorLogging
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.persistence._
import org.ryze.micro.protocol.domain.{DomainCommand, DomainEvent}

import scala.reflect.ClassTag

/**
  * 聚合根
  */
abstract class AggregateRoot[State: ClassTag, Command <: DomainCommand: ClassTag, Event <: DomainEvent: ClassTag] extends PersistentActor with ActorLogging
{
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
    if(lastSequenceNr % AggregateRoot.SNAPSHOT == 0 && lastSequenceNr != 0)
    {
      log info "保存快照"
      saveSnapshot(state.toString)
    }
    updateState(event)
    reply()
    publish(event)
  }

  //1分钟钝化
//  override def preStart(): Unit = context setReceiveTimeout 1.minutes
  override def receiveRecover =
  {
    case SnapshotOffer(m, s: State) => log info s"Loading snapshot at: ${m.sequenceNr} with state: $s"
      log info s"Updated state to $state with snapshot"
      state = s
    case RecoveryCompleted          => log info s"RecoveryCompleted at: $lastSequenceNr with state: $state"
    case event: Event               => updateState(event)
  }
}

object AggregateRoot
{
  //10事件 => 1快照
  final val SNAPSHOT = 2
}
