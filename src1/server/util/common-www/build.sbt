Common.settingsOrg

name := "common-www"

version := "0.0.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation")

enablePlugins(PlayScala)

// use the standard directory layout instead of Play's custom
disablePlugins(PlayLayoutPlugin)

libraryDependencies ++= {
  Seq(
    "com.typesafe.play"   %% "play-slick" % Common.Vsn.PLAY_SLICK
    //"org.scalatest"       %% "scalatest"            % Common.scalaTestVsn % "test"
  )
}

