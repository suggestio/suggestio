Common.settingsOrgJS

//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-common"

version := "0.0.0"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    "com.github.japgolly.scalajs-react" %%% "core"    % Common.reactSjsVsn,
    // Reusability:
    "com.github.japgolly.scalajs-react" %%% "extra"   % Common.reactSjsVsn,
    "com.github.japgolly.scalacss" %%% "ext-react"    % Common.Vsn.SCALACSS,
    "io.suzaku"     %%% "diode-react"                 % Common.diodeReactVsn,
)

npmDependencies in Compile ++= Seq(
  "react"     -> Common.reactJsVsn,
  "react-dom" -> Common.reactJsVsn
)

useYarn := true
