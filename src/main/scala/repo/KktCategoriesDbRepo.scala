package io.ofd.report
package repo

import doobie.implicits.toSqlInterpolator
import doobie.util.transactor
import io.github.gaelrenoux.tranzactio.DbException
import io.github.gaelrenoux.tranzactio.doobie.{Connection, TranzactIO, tzio}
import zio.{Task, ULayer, ZIO, ZLayer}


object KktCategoriesDbRepo {

  trait Service {
    val list: TranzactIO[List[KktCategories]]
  }


  val live: ULayer[KktCategoriesDb] = ZLayer.succeed(new Service {
    override val list:
      ZIO[transactor.Transactor[Task], DbException, List[KktCategories]] = tzio {
      sql"""SELECT * FROM kkt_categories""".query[KktCategories].to[List]
    }
  })

  val list: ZIO[KktCategoriesDb with Connection, DbException, List[KktCategories]] = ZIO.serviceWithZIO[KktCategoriesDb](_.list)
}
