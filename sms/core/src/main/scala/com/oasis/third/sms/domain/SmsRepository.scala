package com.oasis.third.sms.domain

import scala.concurrent.Future

trait SmsRepository
{
  def insert(d: Sms): Future[Boolean]
}
