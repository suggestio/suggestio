organization := "io.suggest"

name := "mbill2"

version := "0.0.0-SNAPSHOT"

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= Seq(
  "typesafe-releases" at "https://ivy2-internal.cbca.ru/artifactory/typesafe-releases",
  "sonatype-oss-releases" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-releases"
)


libraryDependencies ++= {
  val slickVsn      = "3.1.0"
  val slickPgVsn    = "0.10.1"
  val playVsn       = "2.4.6"
  Seq(
    "com.google.inject"     %  "guice"                % "4.0",
    "joda-time"             %  "joda-time"            % "2.8.+",
    "io.suggest"            %% "common"               % "0.0.0-SNAPSHOT",
    "io.suggest"            %% "common-slick-driver"  % "0.0.0-SNAPSHOT",
    "io.suggest"            %% "util"                 % "2.0.1-SNAPSHOT"
      exclude("org.jruby", "jruby-complete")
      exclude("org.slf4j", "slf4j-log4j12")
      exclude("log4j", "log4j")
      exclude("org.slf4j", "log4j-over-slf4j"),
    "io.suggest"            %% "logs-macro"           % "0.0.0-SNAPSHOT"
    //"org.scalatest"       %% "scalatest"            % "2.2.+" % "test"   // Потом надо на 2.3 переключится.
  )
}

