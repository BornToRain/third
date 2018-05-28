package org.ryze.micro.core.http

import akka.http.scaladsl.server.{Directives, Route}

/**
  * RestApi支持
  * Json    => circe
  * HttpDSL => akka-http
  */
trait RestApi extends Directives with JsonSupport
{
  def route: Route
}
