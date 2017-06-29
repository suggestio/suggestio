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
    "org.locationtech.spatial4j"  % "spatial4j"   % Common.Vsn.SPATIAL4J,
    "com.vividsolutions"          % "jts-core"    % Common.Vsn.JTS,

    // TEST
    "org.scalatest" %% "scalatest" % Common.scalaTestVsn % Test
  )
}

