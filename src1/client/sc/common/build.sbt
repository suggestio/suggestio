Common.settingsOrgJS

name := "sc-common"

version := "0.0.0-SNAPSHOT"

enablePlugins(ScalaJSBundlerPlugin)

//libraryDependencies ++= Seq(
  //Common.ORG      %%% "common-sjs"          % "0.0.0-SNAPSHOT"
//)

//testFrameworks += new TestFramework("minitest.runner.Framework")

//requiresDOM in Test := true

// Без зависимостей. Всё минималистичненько.
//skip in packageJSDependencies := true

// TODO Надо бы выставлять это на ci, но НЕ выставлять на продакшенах.
//scalacOptions in Compile ++= Seq("-Xelide-below", "WARNING")

// https://scalacenter.github.io/scalajs-bundler/cookbook.html#performance
//enableReloadWorkflow := true

//scalaJSUseMainModuleInitializer := true

