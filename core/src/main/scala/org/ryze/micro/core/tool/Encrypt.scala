package org.ryze.micro.core.tool

import java.security.MessageDigest
import java.util.{Base64 => JBase64}

import scala.language.postfixOps

object SHA1
{
  @inline
  def encode(s: String) = MessageDigest.getInstance("SHA-1").digest(s.getBytes) map (b => (if (b >= 0 & b < 16) "0"
  else "") + (b & 0xff).toHexString) mkString
}

object Base64
{
  @inline
  def encode(s: String) = new String(JBase64.getEncoder.encode(s.getBytes))
  @inline
  def decode(s: String) = new String(JBase64.getDecoder.decode(s.getBytes))
}

object MD5
{
  @inline
  def encode(s: String) = MessageDigest.getInstance("MD5").digest(s.getBytes) map ("%02x".format(_)) mkString
}
