package io.ofd

import io.ofd.report.repo.KktActivityDb
import zio.{RIO, ZLayer}


package object report {
  type Logging = ZLayer[Any, Any, Unit]
  type DB = KktActivityDb
  type Response[T] = RIO[DB, T]
  type AppTask[A] = RIO[DB, A]
  case class ProductNames(brand: String, productNameHash: String)

}
