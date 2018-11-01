Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-components-materialui"

version := "3.3.2"

//testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG      %%% "scalajs-react-common" % "0.0.0-SNAPSHOT",
  "com.olvind"    %%% "scalajs-react-components-macros" % Common.Vsn.SJS_REACT_COMPONENTS
  
  // Готовые биндинги для material-ui:
  //"com.olvind" %%% "scalajs-react-components" % Common.Vsn.SJS_REACT_COMPONENTS

  //"io.monix"      %%% "minitest" % Common.minitestVsn % Test
)

npmDependencies in Compile ++= Seq(
  "@material-ui/core"  -> version.value,
  "@material-ui/icons" -> "3.0.1"
)

