import play.sbt.PlayImport

Common.settingsOrg

name := "mgeo"

version := "0.0.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation")


libraryDependencies ++= {
  Seq(
    // Около-официальная поддержка GeoJSON для play:
    // 1.4.1-SNAPSHOT - т.к. ожидаем мёржа https://github.com/jroper/play-geojson/pull/21
    "com.typesafe.play.extras"  %% "play-geojson" % Common.Vsn.PLAY_GEOJSON,

    // остальное geo
    "org.locationtech.spatial4j"  % "spatial4j"   % Common.Vsn.SPATIAL4J,
    "com.vividsolutions"          % "jts-core"    % Common.Vsn.JTS,

    // TEST
    "org.scalatest" %% "scalatest" % Common.scalaTestVsn % Test
  )
}

