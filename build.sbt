Common.settingsOrg

// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

name := "scalajs-cordova-ble"

version := "0.0.0-SNAPSHOT"

persistLauncher in Compile := false

persistLauncher in Test := false

//testFrameworks += new TestFramework("utest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    //Common.ORG      %%% "common-sjs"       % "0.0.0-SNAPSHOT",
    "org.scala-js"  %%% "scalajs-dom"      % Common.sjsDomVsn
    //"com.lihaoyi"   %%% "utest"            % "0.3.1"  % "test"
)

jsDependencies ++= Seq(
  RuntimeDOM    % "test"
)

