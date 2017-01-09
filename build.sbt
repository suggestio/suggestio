Common.settingsOrg

name := "ipgeobase"

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
    "com.google.inject"     %  "guice"                % "4.0",
    "commons-io"            % "commons-io"            % Common.apacheCommonsIoVsn,
    Common.ORG              %% "util"                 % "2.0.1-SNAPSHOT"
      exclude("org.jruby", "jruby-complete")
      exclude("org.slf4j", "slf4j-log4j12")
      exclude("log4j", "log4j")
      exclude("org.slf4j", "log4j-over-slf4j"),
    Common.ORG              %% "logs-macro"           % "0.0.0-SNAPSHOT"
    //"org.scalatest"       %% "scalatest"            % Common.scalaTestVsn % "test"
  )
}

