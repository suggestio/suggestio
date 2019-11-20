Common.settingsOrgJS

name := "common-sjs"

enablePlugins(ScalaJSBundlerPlugin)

version := "0.0.0"

libraryDependencies ++= Seq(
  // scala.js 1.+
  //"org.scala-js"        %% "scalajs-env-jsdom-nodejs" % "1.0.0-M8",

  "org.scala-js"        %%% "scalajs-dom"         % Common.sjsDomVsn,
  
  // Некоторые небольшие скрипты для личного кабинета зависят от jquery. А тут живёт jq-утиль для них.
  // В будущем можно будет избавиться от них и удалить jquery из проекта.
  "be.doeraene"         %%% "scalajs-jquery"      % Common.sjsJqueryVsn,
  //"com.lihaoyi"         %%% "autowire"            % "0.2.6",
  
  // Диод нужен, т.к. он постепенно появится во всех js-частях системы.
  // Здесь common-утиль для diode.
  // В выдаче оно пока не задействовано, но это и не важно: будет пострипано.
  // Потом сюда же надо и diode-react запихать.
  "io.suzaku"           %%% "diode"               % Common.diodeVsn,

  // Тестирование...
  "io.monix"            %%% "minitest"            % Common.minitestVsn  % Test
)

testFrameworks += new TestFramework("minitest.runner.Framework")

requireJsDomEnv in Test := true

useYarn := true
