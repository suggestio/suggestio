Common.settingsOrg

name := "stat"

version := "0.0.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= {
  import Common.Repo._
  Seq(
    "typesafe-releases"     at TYPESAFE_RELEASES_URL,
    "sonatype-oss-releases" at SONATYPE_OSS_RELEASES_URL
  )
}


libraryDependencies ++= {
  Seq(
    "org.threeten"          % "threeten-extra"        % Common.Vsn.THREETEN_EXTRA,
    "org.scalatest"         %% "scalatest"            % Common.scalaTestVsn         % Test
  )
}

