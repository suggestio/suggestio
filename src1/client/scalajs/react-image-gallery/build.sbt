// https://www.npmjs.com/package/react-image-gallery
// TODO Не пилился этот проект. Т.к. innerHtml() победил в попапе adv-info.

Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-image-gallery"

version := "0.7.15-SNAPSHOT"

persistLauncher in Compile := false

persistLauncher in Test := false

testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG      %%% "scalajs-react-common"  % "0.0.0-SNAPSHOT",
  "io.monix"      %%% "minitest" % Common.minitestVsn % "test"
  // moment
)

npmDependencies in Compile ++= Seq(
  "react"                  -> Common.reactJsVsn,
  "react-image-gallery"    -> "0.7.15"
)

//requiresDOM in Test := true

