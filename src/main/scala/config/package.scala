package io.ofd.report

import pureconfig.ConfigConvert
import pureconfig.generic.semiauto.deriveConvert


package object config {

  type AppConfig      = AppConfig.Config
  type ClientConfig   = DatabaseConfig.Config
  type ServerConfig   = ServerConfig.Config

  object DatabaseConfig {

    final case class Config(url: String, driver: String)

    object Config {

      implicit val convert: ConfigConvert[Config] = deriveConvert
    }
  }

  object ServerConfig {

    final case class Config(rootUrl: String, port: Int, basePath: String, afterBasePath: String)

    object Config {

      implicit val convert: ConfigConvert[Config] = deriveConvert
    }

  }


}
