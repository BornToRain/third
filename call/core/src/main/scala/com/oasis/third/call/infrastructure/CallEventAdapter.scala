package com.oasis.third.call.infrastructure

import akka.persistence.journal.{Tagged, WriteEventAdapter}
import com.oasis.third.call.protocol.CallEvent

class CallEventAdapter extends WriteEventAdapter
{
  override def manifest(event: Any) = ""
  override def toJournal(event: Any) = event match
  {
    case event: CallEvent => Tagged(event, Set(CallEvent.TAG))
  }
}
