Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "sys-edge-edit-sjs"

version := "0.0.0"

scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "com.softwaremill.macwire"     %%  "macros"           % Common.Vsn.MACWIRE % "provided"
)

useYarn := true
