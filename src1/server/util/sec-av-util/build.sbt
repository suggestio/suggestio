Common.settingsOrg

name := "sec-av-util"

version := "0.0.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= {
  Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % Common.scalaTestPlusPlayVsn % Test
      exclude("commons-logging", "commons-logging")
      exclude("org.w3c.css", "sac")
    )
}

