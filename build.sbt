organization := "io.suggest"

name := "main-sjs"

scalaVersion := "2.11.6"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "io.suggest"    %%  "advext-common"       % "0.0.0-SNAPSHOT",
  "be.doeraene"   %%% "scalajs-jquery"      % "0.8.0",
  "io.suggest"    %%% "advext-sjs-runner"   % "0.0.0-SNAPSHOT",
  "io.suggest"    %%% "sjs-common"          % "0.0.0-SNAPSHOT"
  //"org.monifu"  %%% "minitest" % "0.11" % "test",
)

persistLauncher in Compile := true

persistLauncher in Test := false

//testFrameworks += new TestFramework("minitest.runner.Framework")

jsDependencies += RuntimeDOM % "test"

