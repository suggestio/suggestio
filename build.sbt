organization := "io.suggest"

name := "lk-sjs"

scalaVersion := "2.11.6"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "be.doeraene"   %%% "scalajs-jquery"      % "0.8.0",
  "io.suggest"    %%% "lk-adv-ext-sjs"      % "0.0.0-SNAPSHOT",
  "io.suggest"    %%% "common-sjs"          % "0.0.0-SNAPSHOT"
  //"org.monifu"  %%% "minitest" % "0.11" % "test",
)

persistLauncher in Compile := true

persistLauncher in Test := false

//testFrameworks += new TestFramework("minitest.runner.Framework")

jsDependencies += RuntimeDOM % "test"

