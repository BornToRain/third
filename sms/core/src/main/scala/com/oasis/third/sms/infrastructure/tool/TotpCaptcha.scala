package com.oasis.third.sms.infrastructure.tool

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base32

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.math.{pow, BigInt}

sealed trait CryptoAlgorithm
case object HmacSHA512 extends CryptoAlgorithm

object TotpCaptcha
{
  //默认有效期
  private[this] val validTime = 5.minutes
  //默认密钥
  private[this] val key       = "-RYZE"
  //验证码长度
  private[this] val length    = 4

  @inline
  private[this] def getTotp(secret: String, timeStamp: Long)  =
  {
    val timeWindow   = timeStamp / (validTime toMillis)
    val crypto       = HmacSHA512
    val msg          = BigInt(timeWindow).toByteArray.reverse.padTo(8, 0.toByte).reverse
    val hash         = hmacSha(crypto toString,new Base32() decode secret, msg)
    val offset       = hash(hash.length - 1) & 0xf
    val binary: Long = ((hash(offset) & 0x7f) << 24) |
    ((hash(offset + 1) & 0xff) << 16) |
    ((hash(offset + 2) & 0xff) << 8 |
    (hash(offset + 3) & 0xff))
    val otp          = binary % pow(10, length).toLong

    ("0" * length + otp toString) takeRight length
  }
  @inline
  private def hmacSha(crypto: String, keyBytes: Array[Byte], text: Array[Byte]) =
  {
    val hmac   = Mac getInstance crypto
    val macKey = new SecretKeySpec(keyBytes, "RAW")

    hmac init macKey
    hmac doFinal text
  }

  /**
    * 获取当前时间验证码
    */
  @inline
  final def getCaptcha(mobile: String, `type`: String) = getTotp(s"$key-$mobile-${`type` toUpperCase}", System.currentTimeMillis)
  /**
    * 获取指定时间验证码
    */
  @inline
  final def getCaptchaAtTime(mobile: String, `type`: String, timeStamp: Long) = getTotp(s"$key-$mobile-${`type` toUpperCase}", timeStamp)
}
