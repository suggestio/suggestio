Common.settingsOrgJS

//enablePlugins(ScalaJSPlugin)
enablePlugins(ScalaJSBundlerPlugin)

name := "lk-dt-period-sjs"

version := "0.0.0"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    Common.ORG      %%% "lk-common-sjs"                   % "0.0.0",
    "org.scala-js"  %%% "scalajs-dom"                     % Common.sjsDomVsn
)

useYarn := true
