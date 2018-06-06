package com.oasis.third.call.interfaces.api.v1

import java.net.URLDecoder

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.pattern._
import com.oasis.third.call.Result
import com.oasis.third.call.protocol.CallCommand.{HangUp, Update}
import com.oasis.third.call.protocol.CallRequest.Bind
import com.oasis.third.call.protocol.CallState.GetStateBy
import org.ryze.micro.core.actor.{ActorL, ActorRuntime}
import org.ryze.micro.core.http.RestApi
import org.ryze.micro.core.tool.{ConfigLoader, DateTool}

import scala.util.{Failure, Success}

class CallApi(service: ActorRef)(implicit runtime: ActorRuntime) extends RestApi with ActorL
{
  import runtime._

  Http(runtime.as) bindAndHandle (route, CallApi.host, CallApi.port) onComplete
  {
    case Success(d) => log info  s"电话模块启动成功: ${d.localAddress}"
    case Failure(e) => log error s"电话模块启动失败: ${e.getMessage}"
  }

  /**
    * 将QueryString字符串转成Map
    */
  @inline
  private[this] def convertMap(qs: String) = (Map.empty[String, String] /: (qs split "&" map (_ split "=")))
  {
    case (map, Array(x, y)) => map + (x -> y)
    case (map, _)           => map
  }
  /**
    * Map转实体
    */
  @inline
  private[this] def formMap(map: Map[String, String]) = Update(
    call        = map("CallNo"),
    to          = map("CalledNo"),
    `type`      = map.get("CallType"),
    ringTime    = map.get("Ring") map (DateTool parse _),
    beginTime   = map.get("Begin") map (DateTool parse _),
    endTime     = map.get("End") map (DateTool parse _),
    status      = map.get("State"),
    eventStatus = map.get("CallState"),
    recordFile  = map.get("RecordFile"),
    fileServer  = map.get("FileServer"),
    callId      = map.get("CallID")
  )

  override def route = logRequestResult(("call", Logging.InfoLevel))
  {
    pathPrefix("v1" / "call")
    {
      pathEnd
      {
        //绑定
        (post & entity(as[Bind]))
        {
          r => onSuccess((service ? r).mapTo[Result[String]])
          {
            case Right(d) => complete(Created -> Map("data" -> d))
            case Left(e)  => complete(BadRequest -> e)
          }
        } ~
        //容联七陌电话回调
        (get & parameter('mobile))
        {
          r => onSuccess((service ? GetStateBy(r)).mapTo[Option[String]])
          {
            case Some(d) => complete(d)
            //容联七陌那边需要返回http 200OK 然后内容字符串404来判断没有手机号,脱裤子放屁.
            case _       => complete("404")
          }
        }
      } ~
      pathPrefix("hang-up")
      {
        //挂断电话
        (path(Segment) & delete)
        {
          r => onSuccess((service ? HangUp(r)).mapTo[Result[String]])
          {
            case Right(d) => complete(NoContent)
            case Left(e)  => complete(NotFound)
          }
        } ~
        //更新通话
        (get & extractUri)
        {
          uri => complete
          {
            val map = convertMap(URLDecoder decode (uri.rawQueryString.get, "UTF-8"))
            log info "+---------------------------------------------------------------------------------------------------------------------------+"
            map.toList.sorted foreach { case (k, v) => log.info(s"$k: $v") }
            log info "+---------------------------------------------------------------------------------------------------------------------------+"
            val cmd = formMap(map)
            service ! cmd
            OK
          }
        }
      }
    }
  }
  override def receive = Actor.emptyBehavior
}


object CallApi extends ConfigLoader
{
  final val NAME = "call-api"

  private[this] val httpConfig = loader getConfig "http"

  @inline
  final def props(service: ActorRef)(implicit runtime: ActorRuntime) = Props(new CallApi(service))

  lazy val host = httpConfig getString "host"
  lazy val port = httpConfig getInt "port"
}