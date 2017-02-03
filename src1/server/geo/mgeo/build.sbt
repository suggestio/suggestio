import play.sbt.PlayImport

Common.settingsOrg

name := "mgeo"

version := "0.0.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation")


libraryDependencies ++= {
  Seq(
    // Полу-официальная поддержка GeoJSON для play:
    "com.typesafe.play.extras" %% "play-geojson" % "1.4.+",
    // остальное geo
    "com.spatial4j" % "spatial4j" % "0.4.+",
    "com.vividsolutions" % "jts" % "1.13",

    // TEST
    "org.scalatest" %% "scalatest" % Common.scalaTestVsn % Test
  )
}

