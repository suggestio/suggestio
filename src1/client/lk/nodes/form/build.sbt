Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)
enablePlugins(ScalaJSBundlerPlugin)

name := "lk-nodes-sub-sjs"

version := "0.0.0-SNAPSHOT"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG          %%% "lk-adv-common-sjs"       % "0.0.0-SNAPSHOT",
  Common.ORG          %%% "scalajs-react-common"    % "0.0.0-SNAPSHOT",
  "org.scala-js"      %%% "scalajs-dom"             % Common.sjsDomVsn
)

