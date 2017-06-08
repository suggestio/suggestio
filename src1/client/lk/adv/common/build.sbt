Common.settingsOrgJS

enablePlugins(ScalaJSPlugin)

name := "lk-adv-common-sjs"

version := "0.0.0-SNAPSHOT"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    Common.ORG      %%% "lk-common-sjs"           % "0.0.0-SNAPSHOT",
    Common.ORG      %%% "lk-dt-period-sjs"        % "0.0.0-SNAPSHOT",
    Common.ORG      %%% "common-sjs"              % "0.0.0-SNAPSHOT",
    "org.scala-js"  %%% "scalajs-dom"             % Common.sjsDomVsn
)

