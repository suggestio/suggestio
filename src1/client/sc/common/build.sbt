Common.settingsOrgJS

name := "sc-common"

version := "0.0.0"

enablePlugins(ScalaJSBundlerPlugin)

useYarn := true

libraryDependencies ++= Seq(
  "com.github.japgolly.scalajs-react" %%% "extra" % Common.reactSjsVsn,
  "com.github.japgolly.scalacss" %%% "ext-react"  % Common.Vsn.SCALACSS,

  "com.softwaremill.macwire"     %% "macros"     % Common.Vsn.MACWIRE % "provided",
)

