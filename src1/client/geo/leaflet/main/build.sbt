Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-leaflet"

version := "0.1s"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG      %%% "common-sjs"       % "0.0.0",
  "org.scala-js"  %%% "scalajs-dom"      % Common.sjsDomVsn,
  "io.monix"      %%% "minitest"         % Common.minitestVsn % Test
)

testFrameworks += new TestFramework("minitest.runner.Framework")

npmDependencies in Compile ++= Seq(
  "leaflet"               -> Common.leafletJsVsn,
  "leaflet.locatecontrol" -> Common.leafletControlLocateJsVsn
)

//npmDevDependencies in Compile ++= Seq(
  // Используется только для leaflet-locatecontrol, чтобы отвязать от window.L окончательно.
  //"imports-loader" -> Common.Vsn.IMPORTS_LOADER_JS,
  //"exports-loader" -> Common.Vsn.EXPORTS_LOADER_JS
  //"string-replace-loader" -> Common.Vsn.STRING_REPLACE_LOADER_JS
//)

useYarn := true
