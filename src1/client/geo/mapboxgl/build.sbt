Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)
enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-mapboxgl"

version := "0.0.0"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    Common.ORG      %%% "common-sjs"       % "0.0.0",
    "org.scala-js"  %%% "scalajs-dom"      % Common.sjsDomVsn
)

// TODO jsDependencies: unresolved dependency: org.webjars.npm#geojson-vt, ...
//"org.webjars.npm" % "mapbox-gl" % "0.16.0" / "0.16.0/dist/mapbox-gl.js",

useYarn := true
