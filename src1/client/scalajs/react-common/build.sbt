Common.settingsOrgJS

//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-common"

version := "0.0.0-SNAPSHOT"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    Common.ORG      %%% "common-sjs"                  % "0.0.0-SNAPSHOT",
    "com.github.japgolly.scalajs-react" %%% "core"    % Common.reactSjsVsn,
    // Reusability:
    "com.github.japgolly.scalajs-react" %%% "extra"   % Common.reactSjsVsn,
    "io.suzaku"     %%% "diode-react"                 % Common.diodeReactVsn
)

npmDependencies in Compile ++= Seq(
  "react"     -> Common.reactJsVsn,
  "react-dom" -> Common.reactJsVsn
)

