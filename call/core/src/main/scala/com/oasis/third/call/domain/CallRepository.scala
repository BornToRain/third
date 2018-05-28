package com.oasis.third.call.domain

import scala.concurrent.Future

trait CallRepository
{
  def insert(d: Call): Future[Boolean]
  def selectOne(id: String): Future[Option[Call]]
  def update(d: Call): Future[Boolean]
}
