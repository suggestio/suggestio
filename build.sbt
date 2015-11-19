organization := "io.suggest"

name := "common-sjs"

scalaVersion := "2.11.7"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.scala-js"        %%% "scalajs-dom"         % "0.8.2",
  "io.suggest"          %%  "common"              % "0.0.0-SNAPSHOT",
  "be.doeraene"         %%% "scalajs-jquery"      % "0.8.1",
  "org.monifu"          %%% "minitest"            % "0.12"            % "test"
)

persistLauncher in Compile := false

persistLauncher in Test := false

testFrameworks += new TestFramework("minitest.runner.Framework")

jsDependencies += RuntimeDOM % "test"

