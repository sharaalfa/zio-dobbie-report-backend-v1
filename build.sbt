ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.9"

val zioVersion            = "2.0.1"
val zioInteropCatsVersion = "3.3.0"
val zioLoggingVersion     = "2.1.0"
val zHttpVersion          = "2.0.0-RC10"
val http4sVersion         = "0.23.15"
val circeVersion          = "0.14.2"
val logbackVersion        = "1.2.11"
val timePitVersion        = "0.10.1"
val pureConfigVersion     = "0.17.0"
val rhoSwaggerVersion     = "0.23.0-RC1"
val tranzactIOVersion     = "4.1.0"
val doobieVersion         = "1.0.0-RC2"
val kantanVersion         = "0.7.0"
val sqliteVersion         = "3.39.3.0"
val jodaTimeVersion       = "2.11.2"


lazy val root = (project in file("."))
  .settings(
    name := "zio-dobbie-report-backend-v1",
    idePackagePrefix := Some("io.ofd.report"),
    libraryDependencies ++= Seq(
      zio("zio"),
      zio("zio-streams"),
      "dev.zio"               %% "zio-interop-cats" % zioInteropCatsVersion,
      "dev.zio"               %% "zio-logging"      % zioLoggingVersion,
      "io.d11"                %% "zhttp"            % zHttpVersion,

      http4s("http4s-dsl"),
      http4s("http4s-ember-server"),
      http4s("http4s-circe"),

      circe("circe-core"),
      circe("circe-generic"),
      circe("circe-parser"),
      circe("circe-refined"),

      "org.http4s"            %% "rho-swagger"      % rhoSwaggerVersion,
      "ch.qos.logback"         % "logback-classic"  % logbackVersion,
      "com.github.pureconfig" %% "pureconfig"       % pureConfigVersion,

      timePit("refined"),
      timePit("refined-cats"),
      timePit("refined-eval"),
      timePit("refined-jsonpath"),
      timePit("refined-pureconfig"),
      timePit("refined-scalacheck"),
      timePit("refined-scalaz"),
      timePit("refined-scodec"),
      timePit("refined-scopt"),
      timePit("refined-shapeless"),


      "org.xerial" % "sqlite-jdbc" % sqliteVersion,

      "io.github.gaelrenoux" %% "tranzactio"        % tranzactIOVersion,
      "org.tpolecat" %% "doobie-core"               % doobieVersion,
      "com.nrinaudo" %% "kantan.csv"                % kantanVersion,

      "joda-time" % "joda-time"                     % jodaTimeVersion,



      zio("zio-test")                     % Test,
      zio("zio-test-sbt")                 % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

def circe(artifact: String):   ModuleID =   "io.circe"       %% artifact % circeVersion
def zio(artifact: String):     ModuleID =   "dev.zio"        %% artifact % zioVersion
def http4s(artifact: String):  ModuleID =   "org.http4s"     %% artifact % http4sVersion
def timePit(artifact: String): ModuleID =   "eu.timepit"     %% artifact % timePitVersion