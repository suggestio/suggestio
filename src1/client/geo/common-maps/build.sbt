Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "maps-sjs"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  Common.ORG          %%% "common-sjs"          % "0.0.0-SNAPSHOT",
  Common.ORG          %%% "scalajs-leaflet"     % "0.1s-SNAPSHOT"
)

