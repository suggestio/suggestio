Common.settingsOrg

name := "stat"

version := "0.0.0"

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= {
  import Common.Repo._
  Seq(
    ("sonatype-oss-releases" at SONATYPE_OSS_RELEASES_URL),
  )
}


libraryDependencies ++= {
  Seq(
    "com.google.inject"             % "guice"                % Common.Vsn.GUICE,
    "com.google.inject.extensions"  % "guice-assistedinject" % Common.Vsn.GUICE,

    "org.threeten"          % "threeten-extra"        % Common.Vsn.THREETEN_EXTRA,
    "org.scalatest"         %% "scalatest"            % Common.scalaTestVsn         % Test
  )
}

