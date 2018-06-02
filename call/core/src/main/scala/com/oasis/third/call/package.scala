package com.oasis.third

import org.ryze.micro.core.domain.{DomainError, DomainName}

package object call
{
  implicit final val APP = DomainName("call")

  type Result[A] = Either[DomainError, A]
}
