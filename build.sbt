organization := "io.suggest"

name := "sc-sjs"

scalaVersion := "2.11.6"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "io.suggest"    %%% "common-sjs"          % "0.0.0-SNAPSHOT"
  //"org.monifu"  %%% "minitest" % "0.11" % "test",
)

persistLauncher in Compile := true

persistLauncher in Test := false

//testFrameworks += new TestFramework("minitest.runner.Framework")

jsDependencies += RuntimeDOM % "test"

// Без зависимостей. Всё минималистичненько.
skip in packageJSDependencies := true

