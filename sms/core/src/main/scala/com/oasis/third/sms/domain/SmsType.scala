package com.oasis.third.sms.domain

sealed trait SmsType
{
  val name: String
}

object SmsType
{
  //注册
  case object Register extends SmsType
  {
    override val name = "register"
  }
  //登录
  case object Login extends SmsType
  {
    override val name = "login"
  }
  //邀请
  case object Invitation extends SmsType
  {
    override val name = "invitation"
  }
  //支付
  case object Payment extends SmsType
  {
    override val name = "name"
  }
  //达人通知
  case object Notice extends SmsType
  {
    override val name = "name"
  }

  lazy val seq = Seq(Register, Login, Invitation, Payment, Notice)

  def apply(name: String) = name match
  {
    case Register.name   => Register
    case Login.name      => Login
    case Invitation.name => Invitation
    case Payment.name    => Payment
    case Notice.name     => Notice
  }
}
