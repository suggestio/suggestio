Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

name := "scalajs-leaflet-markercluster"

version := "0.0.0-SNAPSHOT"

persistLauncher in Compile := false

persistLauncher in Test := false

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG      %%% "scalajs-leaflet"  % "0.1s-SNAPSHOT"
)

jsDependencies ++= {
  val lmcVsn = Common.leafletMarkerClusterJsVsn
  Seq(
    "org.webjars.bower" % "leaflet.markercluster" % lmcVsn
      / s"$lmcVsn/dist/leaflet.markercluster-src.js"
      minified s"$lmcVsn/dist/leaflet.markercluster.js"
      dependsOn s"${Common.leafletJsVsn}/dist/leaflet-src.js",
    //"org.webjars" % "leaflet-markercluster" % "0.4.0"   / "0.4.0/leaflet.markercluster.js",
    
    RuntimeDOM % "test"
  )
}

