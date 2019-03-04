// https://www.npmjs.com/package/react-tinymce
//
// И надо не забывать добавлять tinymce.js в html-исходники:
// https://github.com/instructure-react/react-tinymce#dependency

Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-quill"

version := "0.0.0"

//testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG        %%% "scalajs-react-common"  % "0.0.0"
  //"io.monix"      %%% "minitest" % Common.minitestVsn % "test"
)

npmDependencies in Compile ++= Seq(
  "react-quill"   -> Common.Vsn.REACT_QUILL
)

//requiresDOM in Test := true

