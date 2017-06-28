Common.settingsOrgJS

//enablePlugins(ScalaJSPlugin)
enablePlugins(ScalaJSBundlerPlugin)

name := "lk-common-sjs"

version := "0.0.0-SNAPSHOT"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    Common.ORG      %%% "common-sjs"                  % "0.0.0-SNAPSHOT",
    "org.scala-js"  %%% "scalajs-dom"                 % Common.sjsDomVsn,
    "io.suzaku"     %%% "diode-react"                 % Common.diodeVsn,
    Common.ORG      %%% "scalajs-react-common"        % "0.0.0-SNAPSHOT"
)

