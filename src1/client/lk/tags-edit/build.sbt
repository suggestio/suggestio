Common.settingsOrgJS

//enablePlugins(ScalaJSPlugin)
enablePlugins(ScalaJSBundlerPlugin)

name := "lk-tags-edit-sjs"

version := "0.0.0"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "com.softwaremill.macwire" %% "macros" % Common.Vsn.MACWIRE % "provided",
)

useYarn := true
