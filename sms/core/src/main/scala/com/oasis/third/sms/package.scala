package com.oasis.third

import org.ryze.micro.core.domain.DomainError

package object sms
{
  type Result[A] = Either[DomainError, A]
}
