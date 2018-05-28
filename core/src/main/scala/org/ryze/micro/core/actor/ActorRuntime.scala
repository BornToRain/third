package org.ryze.micro.core.actor

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Actor运行环境所需隐式量
  */
case class ActorRuntime(system: ActorSystem)
{
  implicit val as                   = system
  implicit val timeout              = Timeout(5.seconds)
  implicit val materializer         = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
}
