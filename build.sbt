Common.settingsOrg

// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

name := "datetimepicker-scalajs"

version := "0.0.0-SNAPSHOT"

persistLauncher in Compile := false

persistLauncher in Test := false

//testFrameworks += new TestFramework("utest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    "org.scala-js"  %%% "scalajs-dom"       % "0.8.2",
    "be.doeraene"   %%% "scalajs-jquery"    % "0.8.1"
    //"com.lihaoyi"   %%% "utest"            % "0.3.1"  % "test"
)

