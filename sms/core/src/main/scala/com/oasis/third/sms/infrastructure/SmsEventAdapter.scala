package com.oasis.third.sms.infrastructure

import akka.persistence.journal.{Tagged, WriteEventAdapter}
import com.oasis.third.sms.protocol.SmsEvent

class SmsEventAdapter extends WriteEventAdapter
{
  override def manifest(event: Any) = ""
  override def toJournal(event: Any) = event match
  {
    case e: SmsEvent => Tagged(e, Set(SmsEvent.TAG))
  }
}
