organization := "io.suggest"

name := "advext-sjs-runner"

scalaVersion := "2.11.5"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "io.suggest"    %% "advext-common"  % "0.0.0-SNAPSHOT",
  "org.scala-js"  %%% "scalajs-dom"   % "0.8.0"
  //"com.lihaoyi"   %%% "upickle"       % "0.2.+"
)

persistLauncher in Compile := true

