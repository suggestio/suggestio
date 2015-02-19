enablePlugins(ScalaJSPlugin)

organization := "io.suggest"

name := "advext-sjs-runner"

scalaVersion := "2.11.5"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.8.0",
  "com.lihaoyi" %%% "upickle" % "0.2.+"
)

