organization := "io.suggest"

name := "advext-sjs-runner"

scalaVersion := "2.11.6"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "io.suggest"    %% "advext-common"  % "0.0.0-SNAPSHOT",
  //"org.scala-js"  %%% "scalajs-dom"   % "0.8.0",
  "be.doeraene" %%% "scalajs-jquery" % "0.8.0",
  //"com.lihaoyi"   %%% "upickle"       % "0.2.+",
  "org.monifu" %%% "minitest" % "0.11" % "test"
)

persistLauncher in Compile := true

testFrameworks += new TestFramework("minitest.runner.Framework")

// требуется DOM в тестах. http://www.scala-js.org/doc/tutorial.html
jsDependencies += RuntimeDOM

