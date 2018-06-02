package com.oasis.third.pa.interfaces.api.v1

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.{BadRequest, Created}
import akka.pattern._
import com.oasis.third.pa.infrastructure.service.PAClient
import com.oasis.third.pa.infrastructure.service.OasisClient.{request, response}
import com.oasis.third.pa.protocol.PAValueObject
import com.oasis.third.pa.protocol.PARequest.Pay
import com.paic.palife.common.util.encry.la.LASecurityUtils
import io.circe.Json
import io.circe.parser.parse
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}
import org.ryze.micro.core.http.RestApi
import org.ryze.micro.core.tool.ConfigLoader

import scala.util.{Failure, Success}

/**
  * 平安金管家接口
  */
class PAApi(client: ActorRef, oasis: ActorRef)(implicit runtime: ActorRuntime) extends ActorL with RestApi
{
  import runtime._

  Http(runtime.as) bindAndHandle (route, PAApi.host, PAApi.port) onComplete
  {
    case Success(d) => log info s"平安金管家模块启动成功: ${d.localAddress}"
    case Failure(e) => log error s"平安金管家模块启动失败: ${e.getMessage}"
  }

  override def receive = Actor.emptyBehavior
  override def route = logRequestResult(("pa", Logging.InfoLevel))
  {
    pathPrefix("v1" / "pa")
    {
      pathEnd
      {
        //入口
        (get & parameters('merchantCode, 'securityParam, 'securityKey))
        {
          (_, b, c) =>
          val data = parse(LASecurityUtils decryptAESWithRSA (PAClient.publicKey, c, b)) getOrElse Json.Null
          log info s"data: $data"
          complete(data.hcursor.downField("user").as[PAValueObject.User] match
          {
            case Right(d) => (oasis ? request.Register(d.openId)).mapTo[response.Register]
            case _        => BadRequest -> """{"code": 0, "msg": "登录失败!"}"""
          })
        }
      } ~
      path("payments")
      {
        //订单回传
        (post & entity(as[Pay]))
        {
          r =>
            client ! r
            complete(Created)
        }
      }
    }
  }
}

object PAApi extends ConfigLoader
{
  final val NAME = "pa-api"

  private[this] val httpConfig = loader getConfig "http"

  @inline
  def props(client: ActorRef, oasis: ActorRef)(implicit runtime: ActorRuntime) = Props(new PAApi(client, oasis))

  lazy val host = httpConfig getString "host"
  lazy val port = httpConfig getInt "port"
}