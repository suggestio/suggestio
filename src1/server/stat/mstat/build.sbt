Common.settingsOrg

name := "stat"

version := "0.0.0"

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= {
  import Common.Repo._
  Seq(
    ("typesafe-releases"     at TYPESAFE_RELEASES_URL).withAllowInsecureProtocol(true),
    ("sonatype-oss-releases" at SONATYPE_OSS_RELEASES_URL).withAllowInsecureProtocol(true)
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

