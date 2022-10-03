package io.ofd.report

import config.AppConfig
import javax.sql.DataSource

import doobie.util.transactor
import io.github.gaelrenoux.tranzactio.ErrorStrategies
import org.sqlite.SQLiteDataSource
import zio.{Layer, Task, ZIO, ZLayer, durationInt}

package object repo {

  type KktActivityDb = KktActivityDbRepo.Service
  type KktCategoriesDb = KktCategoriesDbRepo.Service
  type KktInfoDb = KktInfoDbRepo.Service
  type SalesDb = SalesDbRepo.Service
  type TransactorC = transactor.Transactor[Task]

  case class KktActivity(kktNumber: String, receiptDateMin: String, receiptDateMax: String)

  case class KktCategories(kktNumber: String, category: String, dateFrom: String, dateTill: String, version: String)

  case class KktInfo(orgInn: String, shopId: Int, kktNumber: String, region: String, dateFrom: String, dateTill: String)

  case class KktInfoEx(orgInn: String, channel: String, region: String)

  case class Sales(orgInn: String, kktNumber: String, receiptDate: String, receiptId: Int, productHashName: String, totalSum: Int)

  case class SalesEx(productHashName: String, receiptDate: String, region: String, channel: String, totalSum: Int)

  case class Conf(
                   dbRecovery: ErrorStrategies,
                   alternateDbRecovery: ErrorStrategies
                 )

  object Conf {
    def live: Layer[AppConfig, Conf] = ZLayer.succeed(
      Conf(
        dbRecovery = ErrorStrategies.timeout(10.seconds).retryForeverExponential(10.seconds, maxDelay = 10.seconds),
        alternateDbRecovery = ErrorStrategies.timeout(10.seconds).retryCountFixed(3, 3.seconds)
      )
    )
  }

  object ConnectionPool {

    val live: ZLayer[AppConfig, Throwable, DataSource] = ZLayer.fromZIO(
      ZIO.serviceWithZIO[AppConfig] { cfg =>
        ZIO.attemptBlocking {
          val ds = new SQLiteDataSource
          ds.setUrl(cfg.db.url)
          ds
        }
      }
    )

  }

}
