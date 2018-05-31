package com.oasis.third.wechat.interfaces.dto

import scala.xml.Elem

sealed trait WechatDTO
{
  val ToUserName  : String
  val FromUserName: String
  val CreateTime  : String
  val MsgType     : String
  val MsgId       : String
}

//文本消息
case class Text
(
  ToUserName  : String,
  FromUserName: String,
  CreateTime  : String,
  MsgType     : String = "text",
  MsgId       : String,
  Content     : String
) extends WechatDTO

//事件消息
case class Event
(
  ToUserName  : String,
  FromUserName: String,
  CreateTime  : String,
  MsgType     : String = "event",
  MsgId       : String,
  Event       : String,
  EventKey    : Option[String],
  Ticket      : Option[String],
  Latitude    : Option[String],
  Longitude   : Option[String],
  Precision   : Option[String]
) extends WechatDTO

object WechatDTO
{
  def apply(xml: Elem) =
  {
    val app        = (xml \ "ToUserName").text
    val openId     = (xml \ "FromUserName").text
    val createTime = (xml \ "CreateTime").text
    val msgType    = (xml \ "MsgType").text
    val msgId      = (xml \ "MsgId").text

    msgType match
    {
      //文本类型
      case "text"  => Text(app, openId, createTime, MsgId = msgId, Content = (xml \ "Content").text)
      //事件类型
      case "event" => (xml \ "Event").text match
      {
        //关注
        case a@"subscribe"                    => Event(app, openId, createTime, MsgId = msgId, Event = a,
          EventKey = Option((xml \ "EventKey").text), Ticket = Option((xml \ "Ticket").text), Latitude = None, Longitude = None, Precision = None)
        //上报地址位置
        case a@"LOCATION"                     => Event(app, openId, createTime, MsgId = msgId, Event = a, EventKey = None, Ticket = None,
          Latitude = None, Longitude = None, Precision = None)
        //菜单事件 CLICK VIEW
        case a if a == "CLICK" || a == "VIEW" => Event(app, openId, createTime, MsgId = msgId, Event = a,
          EventKey = Option((xml \ "EventKey").text), Ticket = None, Latitude = None, Longitude = None, Precision = None)
      }
    }
  }
}