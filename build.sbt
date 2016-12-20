Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

name := "scalajs-mapboxgl"

version := "0.0.0-SNAPSHOT"

persistLauncher in Compile := false

persistLauncher in Test := false

//testFrameworks += new TestFramework("utest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    Common.ORG      %%% "common-sjs"       % "0.0.0-SNAPSHOT",
    "org.scala-js"  %%% "scalajs-dom"      % Common.sjsDomVsn
    //"com.lihaoyi"   %%% "utest"            % "0.3.1"  % "test"
)

jsDependencies ++= Seq(
  // TODO unresolved dependency: org.webjars.npm#geojson-vt, ...
  //"org.webjars.npm" % "mapbox-gl" % "0.16.0" / "0.16.0/dist/mapbox-gl.js",
  RuntimeDOM % "test"
)

