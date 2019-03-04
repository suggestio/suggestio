Common.settingsOrgJS

//enablePlugins(ScalaJSPlugin)
enablePlugins(ScalaJSBundlerPlugin)

name := "lk-common-sjs"

version := "0.0.0"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    Common.ORG      %%% "common-sjs"                  % "0.0.0",
    "org.scala-js"  %%% "scalajs-dom"                 % Common.sjsDomVsn,
    "io.suzaku"     %%% "diode-react"                 % Common.diodeVsn,
    "com.github.japgolly.scalacss" %%% "ext-react"    % Common.Vsn.SCALACSS,
    "com.softwaremill.macwire"     %% "macros"        % Common.Vsn.MACWIRE % "provided",
    Common.ORG      %%% "scalajs-react-common"        % "0.0.0"
)

