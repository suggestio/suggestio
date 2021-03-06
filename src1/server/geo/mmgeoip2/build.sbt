Common.settingsOrg

// MaxMind GeoIP 2.
name := "mmgeoip2"

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
    "com.google.inject"     %  "guice"                % Common.Vsn.GUICE,
    "com.maxmind.geoip2"    %  "geoip2"               % "2.+",
    Common.ORG              %% "util"                 % "2.0.1"
      exclude("org.jruby", "jruby-complete")
      exclude("org.slf4j", "slf4j-log4j12")
      exclude("log4j", "log4j")
      exclude("org.slf4j", "log4j-over-slf4j")
  )
}

