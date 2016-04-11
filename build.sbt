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
    "org.scala-js"  %%% "scalajs-dom"       % Common.sjsDomVsn,
    "be.doeraene"   %%% "scalajs-jquery"    % Common.sjsJqueryVsn
    //"com.lihaoyi"   %%% "utest"            % "0.3.1"  % "test"
)

