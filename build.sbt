Common.settingsOrg

enablePlugins(ScalaJSPlugin)

name := "lk-adv-direct-sjs"

version := "0.0.0-SNAPSHOT"

persistLauncher in Compile := false

persistLauncher in Test := false

testFrameworks += new TestFramework("utest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    Common.ORG      %%% "lk-common-sjs"           % "0.0.0-SNAPSHOT",
    Common.ORG      %%% "lk-dt-period-sjs"        % "0.0.0-SNAPSHOT",
    "org.scala-js"  %%% "scalajs-dom"             % "0.8.2",
    "com.lihaoyi"   %%% "utest"                   % "0.3.1"  % "test"
)

