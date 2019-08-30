Common.settingsOrgJS

//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "lk-adv-geo-tags-sjs"

version := "0.0.0"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    "com.github.japgolly.scalajs-react" %%% "extra"       % Common.reactSjsVsn
)

useYarn := true
