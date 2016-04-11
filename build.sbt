Common.settingsOrg

// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

name := "scalajs-leaflet"

version := "0.1s-SNAPSHOT"

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
  RuntimeDOM % "test",
  "org.webjars" % "leaflet" % "0.7.5" / "0.7.5/leaflet.js"
)

