// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

organization := "io.suggest"

name := "scalajs-mapbox"

version := "0.1s-SNAPSHOT"

scalaVersion := "2.11.7"

persistLauncher in Compile := false

persistLauncher in Test := false

testFrameworks += new TestFramework("utest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.8.2",
    "com.lihaoyi" %%% "utest" % "0.3.1" % "test"
)

