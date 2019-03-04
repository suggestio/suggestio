Common.settingsOrg

name := "sec-www-util"

version := "0.0.0"

scalacOptions ++= Seq("-unchecked", "-deprecation")

enablePlugins(PlayScala)

// use the standard directory layout instead of Play's custom
disablePlugins(PlayLayoutPlugin)

libraryDependencies ++= {
  Seq(
    "org.apache.commons" % "commons-lang3" % Common.Vsn.COMMONS_LANG3,
    "com.lambdaworks" % "scrypt" % "1.4.0",

    // bouncy castle используется для шифрования. pg используется для стойкого шифрования с подписью.
    "org.bouncycastle" % "bcpg-jdk15on"   % Common.bcVsn,
    "org.bouncycastle" % "bcmail-jdk15on" % Common.bcVsn,
    "org.bouncycastle" % "bcprov-jdk15on" % Common.bcVsn,
    "io.trbl.bcpg"     % "bcpg-simple-jdk15on" % "1.51.0",

    // test
    "org.scalatestplus.play" %% "scalatestplus-play" % Common.scalaTestPlusPlayVsn % Test
      exclude("commons-logging", "commons-logging")
      exclude("org.w3c.css", "sac")

  )
}

