Common.settingsOrg

name := "common-slick-driver"

version := Common.sioSlickDrvVsn

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= {
  import Common.Repo._
  Seq(
    "typesafe-releases"     at TYPESAFE_RELEASES_URL,
    "sonatype-oss-releases" at SONATYPE_OSS_RELEASES_URL
  )
}


libraryDependencies ++= {
  val slickPgVsn    = "0.11.2"
  Seq(
    "com.google.inject"     %  "guice"                % "4.0",
    "joda-time"             %  "joda-time"            % "2.8.1",
    "com.typesafe.slick"    %% "slick"                % Common.slickVsn,
    "com.github.tminglei"   %% "slick-pg"             % slickPgVsn,
    "com.github.tminglei"   %% "slick-pg_joda-time"   % slickPgVsn
  )
}

