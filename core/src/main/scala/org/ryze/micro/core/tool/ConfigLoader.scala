package org.ryze.micro.core.tool

import com.typesafe.config.ConfigFactory

trait ConfigLoader
{
  lazy val loader = ConfigFactory.load
}