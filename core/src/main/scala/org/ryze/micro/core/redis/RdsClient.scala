package org.ryze.micro.core.redis

import com.typesafe.config.Config
import org.ryze.micro.core.actor.ActorRuntime
import redis.RedisClient

/**
  * Redis客户端
  */
case class RdsClient(config: Config)(implicit runtime: ActorRuntime)
{
  import runtime._

  private[this] val redisConfig = config getConfig "redis"

  lazy val host = redisConfig getString "host"
  lazy val port = redisConfig getInt "port"
  lazy val pwd  = Option(redisConfig getString "password")
  lazy val rds  = RedisClient(host, port, pwd)
}
