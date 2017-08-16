// https://www.npmjs.com/package/react-tinymce
//
// И надо не забывать добавлять tinymce.js в html-исходники:
// https://github.com/instructure-react/react-tinymce#dependency

Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-tinymce"

version := "0.0.0-SNAPSHOT"

//testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % Common.sjsDomVsn
)

npmDependencies in Compile ++= Seq(
  "tinymce"   -> Common.Vsn.TINYMCE
)

//requiresDOM in Test := true

