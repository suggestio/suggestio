Common.settingsOrgJS

name := "sc-common"

version := "0.0.0"

enablePlugins(ScalaJSBundlerPlugin)

useYarn := true

libraryDependencies ++= Seq(
  // Compile-time DI без рантаймового кода (кроме нагенеренной примитивщины):
  "com.softwaremill.macwire"     %% "macros"     % Common.Vsn.MACWIRE % "provided",
)

