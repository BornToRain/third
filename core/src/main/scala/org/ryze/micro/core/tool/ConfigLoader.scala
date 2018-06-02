package org.ryze.micro.core.tool

import com.typesafe.config.ConfigFactory

import scala.language.postfixOps

trait ConfigLoader
{
  lazy val loader = ConfigFactory load
}