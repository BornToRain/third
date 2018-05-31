package org.ryze.micro.protocol.tool

import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import com.google.protobuf.timestamp.Timestamp

object ProtobufTool
{
  private[this] val mills_unit = 1000

  @inline
  def toDate(timestamp: Timestamp) = new Date(timestamp.seconds * mills_unit)
  @inline
  def toTimestamp(dateTime: LocalDateTime) = Timestamp((dateTime atZone ZoneId.systemDefault).toInstant.getEpochSecond)
}
