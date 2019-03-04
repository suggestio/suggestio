// https://www.npmjs.com/package/react-image-gallery

import Common.Vsn.{REACT_IMAGE_GALLERY => VSN}


Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-image-gallery"

version := VSN

//testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG        %%% "scalajs-react-common"  % "0.0.0"
  //"io.monix"      %%% "minitest" % Common.minitestVsn % "test"
)

npmDependencies in Compile ++= Seq(
  //"react"                  -> Common.reactJsVsn,
  "react-image-gallery"    -> VSN
)

//requiresDOM in Test := true

