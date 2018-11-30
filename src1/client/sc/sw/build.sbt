//import org.scalajs.core.tools.javascript.OutputMode

Common.settingsOrgJS

name := "sc-sw-sjs"

version := "0.0.0-SNAPSHOT"

enablePlugins(ScalaJSBundlerPlugin)

libraryDependencies ++= Seq(
  "com.softwaremill.macwire"     %% "macros"     % Common.Vsn.MACWIRE % "provided"
  //"io.monix"      %%% "minitest"            % Common.minitestVsn  % Test
)

//testFrameworks += new TestFramework("minitest.runner.Framework")

// Для ServiceWorker'а допустим только режим Application, т.к. всё должно быть в одном файле (Обычно LibraryOnly() )
webpackBundlingMode in fullOptJS := BundlingMode.Application

//emitSourceMaps := true

//(emitSourceMaps in fullOptJS) := true

// npmDependencies - тут явно запрещены. Только sjs. TODO Сразу падать в error, если задана хоть одна npm-зависимость.
npmDependencies in Compile := Nil

scalaJSUseMainModuleInitializer := true

