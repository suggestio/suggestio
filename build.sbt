Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

name := "scalajs-evothings-util"

version := "0.0.0-SNAPSHOT"

persistLauncher in Compile := false

persistLauncher in Test := false

// Show more comments when using dubious features
scalacOptions += "-feature"

//libraryDependencies ++= Seq(
//    "org.scala-js"  %%% "scalajs-dom"      % Common.sjsDomVsn
//)

jsDependencies ++= Seq(
  ProvidedJS / "util.js",
  ProvidedJS / "eddystone.js"
)

