Common.settingsOrg

name := "n2"

version := "0.0.0"

resolvers ++= {
  import Common.Repo._
  Seq(
    ("typesafe-releases-art"   at TYPESAFE_RELEASES_URL).withAllowInsecureProtocol(true),
    ("sonatype-oss-releases"   at SONATYPE_OSS_RELEASES_URL).withAllowInsecureProtocol(true),
    ("sonatype-oss-snapshots"  at SONATYPE_OSS_SNAPSHOTS_URL).withAllowInsecureProtocol(true)
  )
}


libraryDependencies ++= {
  Seq(
    Common.ORG          %% "util" % "2.0.1"
      exclude("org.jruby", "jruby-complete")
      exclude("org.slf4j", "slf4j-log4j12")
      exclude("log4j", "log4j")
      exclude("org.slf4j", "log4j-over-slf4j"),
    Common.ORG          %% "swfs" % "0.0.0"
      exclude("org.jruby", "jruby-complete")
      exclude("org.slf4j", "slf4j-log4j12")
      exclude("log4j",     "log4j")
      exclude("org.slf4j", "log4j-over-slf4j"),
    // test
    "org.scalatestplus.play" %% "scalatestplus-play" % Common.scalaTestPlusPlayVsn % Test
      exclude("commons-logging", "commons-logging")
      exclude("org.w3c.css", "sac")
  )
}

//testOptions in Test += Tests.Argument("-oF")

// В тестах используется EhCache, который отваливается между тестами.
// http://stackoverflow.com/a/32180497
parallelExecution in Test := false

