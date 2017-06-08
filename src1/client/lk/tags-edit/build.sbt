Common.settingsOrgJS

enablePlugins(ScalaJSPlugin)

name := "lk-tags-edit-sjs"

version := "0.0.0-SNAPSHOT"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    Common.ORG      %%% "lk-common-sjs"           % "0.0.0-SNAPSHOT"
)

