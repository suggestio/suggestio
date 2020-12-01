Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-materialui"

version := "4.11.1"

//testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

npmDependencies in Compile ++= Seq(
  "@material-ui/core"  -> version.value,
  "@material-ui/lab"   -> "4.0.0-alpha.56",
  "@material-ui/icons" -> "4.9.1"
)

useYarn := true
