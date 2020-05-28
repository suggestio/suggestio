Common.settingsOrgJS

name := "sys-sjs"

version := "0.0.0"

enablePlugins(ScalaJSBundlerPlugin)


webpackBundlingMode := BundlingMode.LibraryOnly()
//webpackBundlingMode in fullOptJS := BundlingMode.Application

// TODO scalajs-1.0 Выставить в scalaJSLinkerConfig emitSourceMaps:
//emitSourceMaps := true

//(emitSourceMaps in fullOptJS) := false

scalaJSLinkerConfig in ThisBuild ~= { _.withESFeatures(_.withUseECMAScript2015(true)) }

scalaJSUseMainModuleInitializer := true

useYarn := true
