Common.settingsOrgJS

name := "sys-sjs"

version := "0.0.0"

enablePlugins(ScalaJSBundlerPlugin)


webpackBundlingMode := BundlingMode.LibraryOnly()
//webpackBundlingMode in fullOptJS := BundlingMode.Application

scalaJSLinkerConfig in ThisBuild ~= { _.withESFeatures(_
  .withESVersion( Common.Vsn.ECMA_SCRIPT )
)}

scalaJSUseMainModuleInitializer := true

scalaJSLinkerConfig in fullOptJS ~= { _.withSourceMap(false) }

useYarn := true
