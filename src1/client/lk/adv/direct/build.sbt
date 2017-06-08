Common.settingsOrgJS

enablePlugins(ScalaJSPlugin)

name := "lk-adv-direct-sjs"

version := "0.0.0-SNAPSHOT"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    Common.ORG      %%% "lk-adv-common-sjs"       % "0.0.0-SNAPSHOT"
)

