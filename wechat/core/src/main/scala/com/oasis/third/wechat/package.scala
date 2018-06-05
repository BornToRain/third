package com.oasis.third

import org.ryze.micro.core.domain.{DomainError, DomainName}

package object wechat
{
  implicit final val APP = DomainName("wechat")

  type Result[A] = Either[DomainError, A]
}
