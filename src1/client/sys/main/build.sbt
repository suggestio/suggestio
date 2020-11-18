Common.settingsOrgJS

name := "sys-sjs"

version := "0.0.0"

enablePlugins(ScalaJSBundlerPlugin)


webpackBundlingMode := BundlingMode.LibraryOnly()
//webpackBundlingMode in fullOptJS := BundlingMode.Application

// TODO scalajs-1.0 Выставить в scalaJSLinkerConfig emitSourceMaps:
//emitSourceMaps := true

//(emitSourceMaps in fullOptJS) := false

scalaJSLinkerConfig in ThisBuild ~= { _.withESFeatures(_
  .withUseECMAScript2015(true)
  .withAvoidClasses(true)      // false - Firefox сильнее тормозит из-за ES2015 classes, но размер js'ника значительно меньше.
  .withAvoidLetsAndConsts(true)
)}

scalaJSUseMainModuleInitializer := true

useYarn := true
