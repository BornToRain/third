package org.ryze.micro.core.tool

import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.{Date, Locale}


object DateTool
{
  lazy val SECONDS    = "seconds"
  lazy val MINUTES    = "minutes"
  lazy val HOURS      = "hours"
  lazy val DAYS       = "days"
  lazy val FULLDATE   = "yyyy-MM-dd HH:mm:ss"
  lazy val TIMESTAMP  = "yyyyMMddHHmmss"
  lazy val RFC822DATE = "EEE, dd MMM yyyy HH:mm:ss z"

  /**
    * yyyyMMddHHmmss格式时间戳
    */
  @inline
  final def datetimeStamp = LocalDateTime.now format DateTimeFormatter.ofPattern(TIMESTAMP)
  @inline
  final def timeStamp = System.currentTimeMillis / 1000
  /**
    * 解析日期字符串
    */
  @inline
  final def parse(s: String, pattern: String = FULLDATE) =
    LocalDateTime parse (s, DateTimeFormatter ofPattern(pattern, Locale.US) withZone ZoneId.systemDefault)
  /**
    * 日期格式化
    */
  @inline
  final def format(dateTime: LocalDateTime, pattern: String = FULLDATE) =
    dateTime format (DateTimeFormatter ofPattern(pattern, Locale.US) withZone ZoneId.systemDefault)
  @inline
  final def formatDate(date: Date, pattern: String = FULLDATE) = format(toLocalDateTime(date), pattern)
  @inline
  final def toDate(dateTime: LocalDateTime) = Date from (dateTime atZone ZoneId.systemDefault).toInstant
  @inline
  final def toLocalDateTime(date: Date) = (date.toInstant atZone ZoneId.systemDefault).toLocalDateTime
  /**
    * x与y的时间差
    * x<y?正数:复数 默认单位分钟
    */
  @inline
  final def compare(x: LocalDateTime, y: LocalDateTime, `type`: String = MINUTES) = `type` match
  {
    case SECONDS => ChronoUnit.SECONDS between (x, y)
    case MINUTES => ChronoUnit.MINUTES between (x, y)
    case HOURS   => ChronoUnit.HOURS between (x, y)
    case DAYS    => ChronoUnit.DAYS between (x, y)
  }
}
