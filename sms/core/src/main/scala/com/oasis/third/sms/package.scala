package com.oasis.third

import org.ryze.micro.core.domain.{DomainError, DomainName}

package object sms
{
  implicit final val APP = DomainName("sms")

  type Result[A] = Either[DomainError, A]
}
