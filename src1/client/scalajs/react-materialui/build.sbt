Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-materialui"

version := Common.Vsn.MATERIAL_UI

//testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

npmDependencies in Compile ++= Seq(
  "@material-ui/core"  -> version.value,
  //"@material-ui/styles" -> version.value,   // since 5.0.0-alpha.35
  "@material-ui/lab"   -> version.value,
  "@material-ui/icons" -> Common.Vsn.MATERIAL_UI_ICONS,
  "@emotion/react"     -> Common.Vsn.EMOTION_REACT,
  "@emotion/styled"    -> Common.Vsn.EMOTION_STYLED,
)

useYarn := true
