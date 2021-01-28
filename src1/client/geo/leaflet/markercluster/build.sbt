Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-leaflet-markercluster"

version := "0.0.0"

// Show more comments when using dubious features
scalacOptions += "-feature"

npmDependencies in Compile ++= Seq(
  "@glartek/leaflet.markercluster" -> Common.leafletMarkerClusterJsVsn
)

npmDevDependencies in Compile ++= Seq(
  "imports-loader" -> Common.Vsn.IMPORTS_LOADER_JS,
  "exports-loader" -> Common.Vsn.EXPORTS_LOADER_JS
)

useYarn := true
