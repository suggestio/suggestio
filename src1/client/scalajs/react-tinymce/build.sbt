// https://www.npmjs.com/package/react-tinymce
//
// И надо не забывать добавлять tinymce.js в html-исходники:
// https://github.com/instructure-react/react-tinymce#dependency

Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-tinymce"

version := "0.0.0-SNAPSHOT"

//testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG        %%% "scalajs-react-common"  % "0.0.0-SNAPSHOT"
  //"io.monix"      %%% "minitest" % Common.minitestVsn % Test
)

npmDependencies in Compile ++= Seq(
  "react-tinymce"   -> Common.Vsn.REACT_TINYMCE
)

//requiresDOM in Test := true

