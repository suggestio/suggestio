Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-leaflet-markercluster"

version := "0.0.0-SNAPSHOT"

persistLauncher in Compile := false

persistLauncher in Test := false

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG      %%% "scalajs-leaflet"  % "0.1s-SNAPSHOT"
)

npmDependencies in Compile ++= Seq(
  "leaflet.markercluster" -> Common.leafletMarkerClusterJsVsn
)

