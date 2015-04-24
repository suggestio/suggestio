organization := "io.suggest"

name := "common-sjs"

scalaVersion := "2.11.6"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "be.doeraene" %%% "scalajs-jquery" % "0.8.0"
  //"org.monifu"  %%% "minitest" % "0.11" % "test",
)

persistLauncher in Compile := true

persistLauncher in Test := false

//testFrameworks += new TestFramework("minitest.runner.Framework")

jsDependencies += RuntimeDOM % "test"

