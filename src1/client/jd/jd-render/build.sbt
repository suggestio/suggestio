Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "jd-render-sjs"

version := "0.0.0-SNAPSHOT"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG                     %%% "lk-common-sjs"    % "0.0.0-SNAPSHOT",
  
  "com.softwaremill.macwire"     %%  "macros"           % Common.Vsn.MACWIRE % "provided",

  "com.github.japgolly.scalacss" %%% "ext-react"        % Common.Vsn.SCALACSS
)

