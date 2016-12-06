Common.settingsOrg

enablePlugins(ScalaJSPlugin)

name := "lk-dt-period-sjs"

version := "0.0.0-SNAPSHOT"

persistLauncher in Compile := false

persistLauncher in Test := false

testFrameworks += new TestFramework("utest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    Common.ORG      %%% "lk-common-sjs"                   % "0.0.0-SNAPSHOT",
    Common.ORG      %%% "datetimepicker-scalajs"          % "0.0.0-SNAPSHOT",
    "org.scala-js"  %%% "scalajs-dom"                     % Common.sjsDomVsn,
    "com.lihaoyi"   %%% "utest"                           % "0.3.1"  % "test"
)

