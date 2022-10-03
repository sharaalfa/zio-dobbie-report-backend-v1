package io.ofd.report
package repo

import doobie.implicits.toSqlInterpolator
import doobie.util.transactor
import io.github.gaelrenoux.tranzactio.DbException
import io.github.gaelrenoux.tranzactio.doobie.{Connection, TranzactIO, tzio}
import zio.{Task, ULayer, ZIO, ZLayer}

object KktActivityDbRepo {

  trait Service {
    val list: TranzactIO[List[KktActivity]]
  }


  val live: ULayer[KktActivityDb] = ZLayer.succeed(new Service {
    override val list:
      ZIO[transactor.Transactor[Task], DbException, List[KktActivity]] = tzio{
      sql"""SELECT * FROM kkt_activity""".query[KktActivity].to[List]
    }
  })

  val list: ZIO[KktActivityDb with Connection, DbException, List[KktActivity]] = ZIO.serviceWithZIO[KktActivityDb](_.list)
}
