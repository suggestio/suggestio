Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "maps-sjs"

version := "0.0.0"

libraryDependencies ++= Seq(
  Common.ORG          %%% "common-sjs"          % "0.0.0",
  Common.ORG          %%% "scalajs-leaflet"     % "0.1s"
)

useYarn := true
