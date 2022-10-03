package io.ofd.report

import config.AppConfig
import repo._

import cats.data.Kleisli
import com.comcast.ip4s.Port
import io.github.gaelrenoux.tranzactio.DbException
import io.github.gaelrenoux.tranzactio.doobie.{Connection, Database}
import org.http4s.Request
import org.http4s.Uri.Ipv4Address
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import zio._
import zio.interop.catz.asyncInstance
import zio.logging.LogFormat


object ZioServer extends ZIOAppDefault {


  private[this] val logger: ZLayer[Any, Any, Unit] = Runtime
    .removeDefaultLoggers >>> zio
    .logging.console(LogFormat.default)


  private[this] val getAppConfig: URIO[AppConfig, AppConfig.Config] = ZIO.environmentWith(_.get)

  private[this] def routes(appConfig: AppConfig,
                           logging: Logging,
                           kktActivities: List[KktActivity],
                           kktCategories: List[KktCategories],
                           kktInfo: List[KktInfo],
                           sales: List[Sales]):
  Kleisli[Response, Request[Response], org.http4s.Response[Response]] = Router[Response](
    s"/${appConfig.server.basePath}" -> Api(logging, appConfig, kktActivities, kktCategories, kktInfo, sales).routes
  ).orNotFound

  private val conf = AppConfig.live ++ Conf.live
  private val dbRecoveryConf = conf >>> ZLayer.fromFunction((_: Conf).dbRecovery)
  private val datasource = conf >>> ConnectionPool.live
  private val database = (datasource ++ dbRecoveryConf) >>> Database.fromDatasourceAndErrorStrategies
  private val kktActivityDbRepo = KktActivityDbRepo.live
  private val kktCategoriesDbRepo = KktCategoriesDbRepo.live
  private val kktInfoDbRepo = KktInfoDbRepo.live
  private val salesDbRepo = SalesDbRepo.live

  type AppEnvActivity = Database with KktActivityDb with AppConfig
  type AppEnvCategories = Database with KktCategoriesDb with AppConfig
  type AppEnvInfo = Database with KktInfoDb with AppConfig
  type AppEnvSales = Database with SalesDb with AppConfig

  private val appEnvActivity = database ++ kktActivityDbRepo ++ conf
  private val appEnvCategories = database ++ kktCategoriesDbRepo ++ conf
  private val appEnvInfo = database ++ kktInfoDbRepo ++ conf
  private val appEnvSales = database ++ salesDbRepo ++ conf

  val cfg: ZIO[Any, IllegalStateException, AppConfig.Config] = {
    val config = for {
      cfg <- getAppConfig
    } yield cfg
    config.provideSomeLayer(AppConfig.live)
  }
  val app: ZIO[Any with Scope, Throwable, ExitCode] = {

    val layer = conf ++ database ++ kktActivityDbRepo ++ logger

    val serve = for {
      cfg <- cfg.provideSomeLayer(AppConfig.live)
      kktActivities <- activityApp().provideLayer(appEnvActivity)
      kktCategories <- categoriesApp().provideLayer(appEnvCategories)
      kktInfo <- infoApp().provideLayer(appEnvInfo)
      sales <- salesApp().provideLayer(appEnvSales)
      _ <- ZIO
        .logInfo(s"Start server on ${cfg.server.rootUrl}:${cfg.server.port}")
      server <- ZIO.runtime[KktActivityDb].flatMap { implicit rts =>
        EmberServerBuilder.default[AppTask]
          .withHost(Ipv4Address
            .unsafeFromString(cfg.server.rootUrl)
            .address)
          .withPort(Port
            .fromInt(cfg.server.port)
            .get)
          .withHttpApp(routes(
            cfg,
            logger,
            kktActivities,
            kktCategories,
            kktInfo,
            sales)
          )
          .build
          .useForever
      }
    } yield server

    serve
      .provideSomeLayer(layer)
      .foldZIO(
        failure =>
          ZIO
            .logError(failure.toString),
        _ =>
          ZIO
            .logInfo("Success")
      ).exitCode
  }

  private def activityApp(): ZIO[AppEnvActivity, DbException, List[KktActivity]] = {
    val queries: ZIO[Connection with AppEnvActivity, DbException, List[KktActivity]] = for {
      trio <- KktActivityDbRepo.list
    } yield trio

    ZIO.serviceWithZIO[AppConfig] { conf =>
      Database.transactionOrWiden(queries)
    }
  }

  private def categoriesApp(): ZIO[AppEnvCategories, DbException, List[KktCategories]] = {
    val queries: ZIO[Connection with AppEnvCategories, DbException, List[KktCategories]] = for {
      trio <- KktCategoriesDbRepo.list
    } yield trio

    ZIO.serviceWithZIO[AppConfig] { conf =>
      Database.transactionOrWiden(queries)
    }
  }

  private def infoApp(): ZIO[AppEnvInfo, DbException, List[KktInfo]] = {
    val queries: ZIO[Connection with AppEnvInfo, DbException, List[KktInfo]] = for {
      trio <- KktInfoDbRepo.list
    } yield trio

    ZIO.serviceWithZIO[AppConfig] { conf =>
      Database.transactionOrWiden(queries)
    }
  }

  private def salesApp(): ZIO[AppEnvSales, DbException, List[Sales]] = {
    val queries: ZIO[Connection with AppEnvSales, DbException, List[Sales]] = for {
      trio <- SalesDbRepo.list
    } yield trio

    ZIO.serviceWithZIO[AppConfig] { conf =>
      Database.transactionOrWiden(queries)
    }
  }

  def run: ZIO[Any with Scope, Throwable, ExitCode] = app
    .tapError(err => ZIO.logError(s"Execution failed with: $err"))

}