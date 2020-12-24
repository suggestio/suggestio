Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-materialui-color"

version := Common.Vsn.MATERIALUI_COLOR

testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "io.monix"      %%% "minitest" % Common.minitestVsn % Test
)

npmDependencies in Compile ++= Seq(
  "material-ui-color"  -> version.value,
  "styled-components"  -> Common.Vsn.STYLED_COMPONENTS,
)

useYarn := true

requireJsDomEnv in Test := true
