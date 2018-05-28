package org.ryze.micro.core.domain

/**
  * 领域错误
  */
case class DomainError(code: Int, msg: String)

object DomainError
{
  lazy val Created  = DomainError(0, "创建失败!")
  lazy val Updated  = DomainError(1, "修改失败!")
  lazy val NotFound = DomainError(2, "数据不存在!")
}
