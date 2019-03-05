Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "cart-sjs"

version := "0.0.0"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "com.softwaremill.macwire"     %%  "macros"           % Common.Vsn.MACWIRE % "provided"
  //"com.github.japgolly.scalacss" %%% "ext-react"        % Common.Vsn.SCALACSS
)

useYarn := true
