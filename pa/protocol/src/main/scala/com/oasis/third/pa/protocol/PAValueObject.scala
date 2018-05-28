package com.oasis.third.pa.protocol

/**
  * 平安金管家领域值对象
  */
trait PAValueObject

object PAValueObject
{
  case class OrderDetail(id: String, name: String, count: Int, uri: Option[String] = Some("null"), imageUri: Option[String] = Some("null"),
    subject: Option[String] = Some("null"), amount: Int) extends PAValueObject
  case class User(openId: String, mobilePhone: Option[String], agent: Option[String], empType: Option[String], alias: Option[String],
    userType: Option[String]) extends PAValueObject
}