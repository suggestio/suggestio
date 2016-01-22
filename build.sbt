Common.settingsOrg

name := "common-slick-driver"

version := "0.0.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= Seq(
  "typesafe-releases" at "https://ivy2-internal.cbca.ru/artifactory/typesafe-releases",
  "sonatype-oss-releases" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-releases"
)


libraryDependencies ++= {
  val slickPgVsn    = "0.10.1"
  Seq(
    "com.google.inject"     %  "guice"                % "4.0",
    "joda-time"             %  "joda-time"            % "2.8.1",
    "com.github.tminglei"   %% "slick-pg"             % slickPgVsn,
    "com.github.tminglei"   %% "slick-pg_joda-time"   % slickPgVsn
  )
}

