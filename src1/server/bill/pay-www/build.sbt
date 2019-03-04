Common.settingsOrg

name := "pay-www"

version := "0.0.0"

scalacOptions ++= Seq("-unchecked", "-deprecation")

enablePlugins(PlayScala)

// use the standard directory layout instead of Play's custom
disablePlugins(PlayLayoutPlugin)

//libraryDependencies ++= {
//  Seq(
    //"com.google.inject"     %  "guice"                % Common.Vsn.GUICE
    //"org.scalatest"       %% "scalatest"            % Common.scalaTestVsn % "test"
//  )
//}

