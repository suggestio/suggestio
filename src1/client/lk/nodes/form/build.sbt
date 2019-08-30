Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)
enablePlugins(ScalaJSBundlerPlugin)

name := "lk-nodes-sub-sjs"

version := "0.0.0"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "org.scala-js"      %%% "scalajs-dom"             % Common.sjsDomVsn,
  "com.softwaremill.macwire" %% "macros"            % Common.Vsn.MACWIRE % "provided"
)

useYarn := true
