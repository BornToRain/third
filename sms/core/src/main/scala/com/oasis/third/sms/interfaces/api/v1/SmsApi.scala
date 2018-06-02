package com.oasis.third.sms.interfaces.api.v1

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.pattern._
import com.oasis.third.sms.Result
import com.oasis.third.sms.protocol.SmsRequest.{Create, Validate}
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}
import org.ryze.micro.core.http.RestApi
import org.ryze.micro.core.tool.ConfigLoader

import scala.util.{Failure, Success}

class SmsApi(service: ActorRef)(implicit runtime: ActorRuntime) extends RestApi with ActorL
{
  import runtime._

  Http(runtime.as) bindAndHandle (route, SmsApi.host, SmsApi.port) onComplete
  {
    case Success(d) => log info s"短信模块启动成功: ${d.localAddress}"
    case Failure(e) => log error s"短信模块启动失败: ${e.getMessage}"
  }

  override def route = logRequestResult(("sms", Logging.InfoLevel))
  {
    pathPrefix("v1" / "sms")
    {
      //创建短信
      (pathEnd & post & entity(as[Create]))
      {
        r => onSuccess((service ? r).mapTo[Result[String]])
        {
          case Right(d) => complete(Created -> Map("data" -> d))
          case Left(e)  => complete(BadRequest -> e)
        }
      } ~
      //校验短信
      (path("validation") & post & entity(as[Validate]))
      {
        r => onSuccess((service ? r).mapTo[Result[Boolean]])
        {
          case Right(d) => complete(d)
          case Left(e)  => complete(BadRequest -> e)
        }
      }
    }
  }
  override def receive = Actor.emptyBehavior
}

object SmsApi extends ConfigLoader
{
  final val NAME = "sms-api"
  @inline
  final def props(service: ActorRef)(implicit runtime: ActorRuntime) = Props(new SmsApi(service))

  private[this] val httpConfig = loader getConfig "http"

  lazy val host = httpConfig getString "host"
  lazy val port = httpConfig getInt "port"
}
