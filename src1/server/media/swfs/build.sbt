Common.settingsOrg

name := "swfs"

version := "0.0.0"

resolvers ++= Seq(
  "typesafe-releases"       at Common.Repo.TYPESAFE_RELEASES_URL,
  "sonatype-oss-releases"   at Common.Repo.SONATYPE_OSS_RELEASES_URL,
  "sonatype-oss-snapshots"  at Common.Repo.SONATYPE_OSS_SNAPSHOTS_URL
)


libraryDependencies ++= {
  Seq(
    "com.google.inject" % "guice" % Common.Vsn.GUICE,

    Common.ORG          %% "util" % "2.0.1"
      exclude("org.jruby", "jruby-complete")
      exclude("org.slf4j", "slf4j-log4j12")
      exclude("log4j", "log4j")
      exclude("org.slf4j", "log4j-over-slf4j"),
    // test
    "org.scalatestplus.play" %% "scalatestplus-play" % Common.scalaTestPlusPlayVsn % "test"
      exclude("commons-logging", "commons-logging")
      exclude("org.w3c.css", "sac")
  )
}

//testOptions in Test += Tests.Argument("-oF")

