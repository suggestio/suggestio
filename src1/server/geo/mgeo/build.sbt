import play.sbt.PlayImport

Common.settingsOrg

name := "mgeo"

version := "0.0.0"

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers += ("jroper-maven" at Common.Repo.JROPER_MAVEN_REPO).withAllowInsecureProtocol(true)

libraryDependencies ++= {
  Seq(
    "au.id.jazzy" %% "play-geojson" % Common.Vsn.PLAY_GEOJSON,

    // остальное geo
    "org.locationtech.spatial4j"  % "spatial4j"   % Common.Vsn.SPATIAL4J,
    "com.vividsolutions"          % "jts-core"    % Common.Vsn.JTS,

    // TEST
    "org.scalatest" %% "scalatest" % Common.scalaTestVsn % Test
  )
}

