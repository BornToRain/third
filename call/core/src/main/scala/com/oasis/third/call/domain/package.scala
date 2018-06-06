package com.oasis.third.call

package object domain
{
  //RedisKey
  final val KEY_BINDING = "binding=>"

  type Domain    = Option[Call]
  type RedisCall = Array[String]
}
