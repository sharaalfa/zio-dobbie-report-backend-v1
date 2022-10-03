package io.ofd.report

import config.AppConfig
import repo._

import io.circe.generic.encoding.ReprAsObjectEncoder.deriveReprAsObjectEncoder
import io.circe.{Decoder, Encoder}
import org.http4s.circe._
import org.http4s.headers.`Content-Type`
import org.http4s.rho.swagger.models.{Info, Tag}
import org.http4s.rho.swagger.{SwaggerFileResponse, SwaggerMetadata, SwaggerSupport}
import org.http4s.rho.{RhoMiddleware, RhoRoutes}
import org.http4s.server.middleware.CORS
import org.http4s.{EntityDecoder, EntityEncoder, Headers, HttpRoutes, MediaType}
import zio._
import zio.interop.catz.asyncInstance


final case class Api[R <: KktActivityDb](logging: Logging,
                                         appConfig: AppConfig,
                                         kktActivities: List[KktActivity],
                                         kktCategories: List[KktCategories],
                                         kktInfo: List[KktInfo],
                                         sales: List[Sales]) {

  type RoutesTask[A] = RIO[R, A]

  implicit def circeJsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[RoutesTask, A] = jsonOf[RoutesTask, A]

  implicit def circeJsonEncoder[A](implicit encoder: Encoder[A]): EntityEncoder[RoutesTask,A] = jsonEncoderOf[RoutesTask, A]

  val metadata: SwaggerMetadata = SwaggerMetadata(
    apiInfo = Info(title = "Ofd Report Backend", version = "1"),
    tags = List(Tag(name = "backend", description = Some("These are the backend routes."))),
    host = Some(s"${appConfig.server.rootUrl}:${appConfig.server.port}"),
    basePath = Some(s"/${appConfig.server.basePath}"),
    produces = List("text/csv")
  )

  val swaggerUiRhoMiddleware: RhoMiddleware[RoutesTask] = SwaggerSupport[RoutesTask]
    .createRhoMiddleware(swaggerMetadata = metadata)

  val routes: HttpRoutes[RoutesTask] = CORS.policy.withAllowOriginAll(
    new RhoRoutes[RoutesTask] {
      GET / s"${appConfig.server.afterBasePath}/report-file" /
        pathVar[String]("pathToFile", "путь к файлу roduct_names.csv") +?
        paramD[String]("dateFrom", "date_from") &
        paramD[String]("dateTo", "date_to") &
        paramD[List[String]]("categories",
          "Фильтр kkt_category, принимающий на вход список категорий: может быть пустым или содержать любое кол-во категорий (например: FMCG, HoReCa)") &
        paramD[Boolean]("receiptDate", false, "Признак необходимости группировки") &
        paramD[Boolean]("region", false, "Признак необходимости группировки") &
        paramD[Boolean]("channel", false, "channel") |>> {
        (pathToFile: String,
         dateFrom: String,
         dateTo: String,
         categories: List[String],
         receiptDate: Boolean,
         region: Boolean,
         channel: Boolean) => Logic
          .reportBy(kktActivities,
            kktCategories,
            kktInfo,
            sales,
            pathToFile,
            dateFrom,
            dateTo,
            categories,
            receiptDate,
            region,
            channel).foldZIO(fail =>
          ZIO
            .logError(fail.toString) *> NotFound(),
          out => ZIO
            .logInfo("Success") *> Ok.apply(SwaggerFileResponse(out),
            Headers(`Content-Type`(MediaType.text.csv)))
        )
      }


//.map(_.withoutContentType)
      //        .map(_.withContentType(contentType = `Content-Type`(MediaType.text.`csv-schema`)))
      //        .map(_.withHeaders(headers=Headers(`Content-Type`(MediaType.text.csv))))
    }.toRoutes(swaggerUiRhoMiddleware)
  )


}
