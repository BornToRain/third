package com.oasis.third.call.infrastructure

import akka.serialization.SerializerWithStringManifest
import com.oasis.third.call.domain.event._

class CallSerializer extends SerializerWithStringManifest
{
  final val BOUND   = classOf[Bound].getName
  final val HUNG_UP = classOf[HungUp].getName
  final val UPDATED = classOf[Updated].getName

  override def identifier = getClass.getName.hashCode
  override def manifest(o: AnyRef) = o.getClass.getName
  override def toBinary(o: AnyRef) = o match
  {
    case o: Bound   => o.toByteArray
    case o: HungUp  => o.toByteArray
    case o: Updated => o.toByteArray
  }
  override def fromBinary(bytes: Array[Byte], manifest: String) = manifest match
  {
    case BOUND   => Bound parseFrom bytes
    case HUNG_UP => HungUp parseFrom bytes
    case UPDATED => Updated parseFrom bytes
  }
}
