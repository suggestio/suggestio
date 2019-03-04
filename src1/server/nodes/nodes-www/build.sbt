Common.settingsOrg

name := "nodes-www"

version := "0.0.0"

scalacOptions ++= Seq("-unchecked", "-deprecation")

enablePlugins(PlayScala)

// use the standard directory layout instead of Play's custom
disablePlugins(PlayLayoutPlugin)

//libraryDependencies ++= {
//  Seq(
//    //"org.scalatest"       %% "scalatest"            % Common.scalaTestVsn % "test"
//  )
//}

