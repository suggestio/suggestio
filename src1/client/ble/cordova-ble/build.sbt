Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

name := "scalajs-cordova-ble"

version := "0.0.0"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "org.scala-js"  %%% "scalajs-dom"      % Common.sjsDomVsn
)

//jsEnv in Test := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv

useYarn := true
