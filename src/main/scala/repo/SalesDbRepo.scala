package io.ofd.report
package repo

import doobie.implicits.toSqlInterpolator
import doobie.util.transactor
import io.github.gaelrenoux.tranzactio.DbException
import io.github.gaelrenoux.tranzactio.doobie.{Connection, TranzactIO, tzio}
import zio.{Task, ULayer, ZIO, ZLayer}


object SalesDbRepo {

  trait Service {
    val list: TranzactIO[List[Sales]]
  }


  val live: ULayer[SalesDb] = ZLayer.succeed(new Service {
    override val list:
      ZIO[transactor.Transactor[Task], DbException, List[Sales]] = tzio {
      sql"""SELECT * FROM sales""".query[Sales].to[List]
    }
  })

  val list: ZIO[SalesDb with Connection, DbException, List[Sales]] = ZIO.serviceWithZIO[SalesDb](_.list)
}
