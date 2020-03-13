Common.settingsOrgJS

name := "sys-sjs"

version := "0.0.0"

enablePlugins(ScalaJSBundlerPlugin)


// Есть плавающая проблема с LibraryOnly и Leaflet.js (global.L):
// https://github.com/scalacenter/scalajs-bundler/issues/178
webpackBundlingMode := BundlingMode.LibraryOnly()
//webpackBundlingMode in fullOptJS := BundlingMode.Application

// TODO scalajs-1.0 Выставить в scalaJSLinkerConfig emitSourceMaps:
emitSourceMaps := true

(emitSourceMaps in fullOptJS) := false


scalaJSUseMainModuleInitializer := true

useYarn := true
