Common.settingsOrg

name := "ipgeobase"

version := "0.0.0"

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= {
  import Common.Repo._
  Seq(
    ("typesafe-releases-art" at TYPESAFE_RELEASES_URL).withAllowInsecureProtocol(true),
    ("sonatype-oss-releases" at SONATYPE_OSS_RELEASES_URL).withAllowInsecureProtocol(true)
  )
}


libraryDependencies ++= {
  Seq(
    //"com.google.inject"             % "guice"                % Common.Vsn.GUICE,
    "com.google.inject.extensions"  % "guice-assistedinject" % Common.Vsn.GUICE,
    "commons-io"            % "commons-io"            % Common.apacheCommonsIoVsn,
    Common.ORG              %% "util"                 % "2.0.1"
      exclude("org.jruby", "jruby-complete")
      exclude("org.slf4j", "slf4j-log4j12")
      exclude("log4j", "log4j")
      exclude("org.slf4j", "log4j-over-slf4j")
  )
}

