Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-materialui"

version := "5.0.0-alpha.24"

//testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

npmDependencies in Compile ++= Seq(
  "@material-ui/core"  -> version.value,
  "@material-ui/lab"   -> version.value,
  "@material-ui/icons" -> version.value,
  "@emotion/react" -> "11.1.4",
  "@emotion/styled" -> "11.0.0"
)

useYarn := true
