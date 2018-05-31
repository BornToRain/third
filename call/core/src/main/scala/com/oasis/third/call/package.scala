package com.oasis.third

import org.ryze.micro.core.domain.DomainError

package object call
{
  final val APP = "call"

  type Result[A] = Either[DomainError, A]
}
