package io.ofd.report
package config

import pureconfig.{ConfigConvert, ConfigSource}
import pureconfig.generic.semiauto.deriveConvert
import zio.{URIO, ZIO, ZLayer}


object AppConfig {

  final case class Config(db: DatabaseConfig.Config, server: ServerConfig.Config)

  val getAppConfig: URIO[AppConfig, AppConfig.Config] = ZIO.environmentWith(_.get)

  object Config {
    implicit val convert: ConfigConvert[Config] = deriveConvert
  }

  val live: ZLayer[Any, IllegalStateException, AppConfig] =
    ZLayer.fromZIO {
      ZIO.logInfo("config load") *>
        ZIO
          .fromEither(ConfigSource.default.load[Config])
         // .tapError(err => ZIO.logError(s"Error loading configuration: $err"))
          .mapError(failures =>
            new IllegalStateException(
              s"Error loading configuration: $failures"
            )
          )
    }
}