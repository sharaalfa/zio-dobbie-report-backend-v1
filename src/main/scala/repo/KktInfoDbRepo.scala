package io.ofd.report
package repo

import doobie.implicits.toSqlInterpolator
import doobie.util.transactor
import io.github.gaelrenoux.tranzactio.DbException
import io.github.gaelrenoux.tranzactio.doobie.{Connection, TranzactIO, tzio}
import zio.{Task, ULayer, ZIO, ZLayer}


object KktInfoDbRepo {

  trait Service {
    val list: TranzactIO[List[KktInfo]]
  }


  val live: ULayer[KktInfoDb] = ZLayer.succeed(new Service {
    override val list:
      ZIO[transactor.Transactor[Task], DbException, List[KktInfo]] = tzio {
      sql"""SELECT * FROM kkt_info""".query[KktInfo].to[List]
    }
  })

  val list: ZIO[KktInfoDb with Connection, DbException, List[KktInfo]] = ZIO.serviceWithZIO[KktInfoDb](_.list)

}
