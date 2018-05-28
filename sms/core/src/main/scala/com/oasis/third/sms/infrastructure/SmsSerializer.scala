package com.oasis.third.sms.infrastructure

import akka.serialization.SerializerWithStringManifest
import com.oasis.third.sms.domain.event.Created

class SmsSerializer extends SerializerWithStringManifest
{
  final val CREATED = classOf[Created].getName

  override def identifier = getClass.getName.hashCode
  override def manifest(o: AnyRef) = o.getClass.getName
  override def toBinary(o: AnyRef) = o match
  {
    case e: Created => e.toByteArray
  }
  override def fromBinary(bytes: Array[Byte], manifest: String) = manifest match
  {
    case CREATED => Created parseFrom bytes
  }
}
