Common.settingsOrgJS

//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "lk-adv-geo-tags-sjs"

version := "0.0.0"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    Common.ORG      %%% "lk-adv-common-sjs"               % "0.0.0",
    Common.ORG      %%% "scalajs-react-common"            % "0.0.0",
    Common.ORG      %%% "maps-sjs"                        % "0.0.0",
    Common.ORG      %%% "lk-tags-edit-sjs"                % "0.0.0",
    Common.ORG      %%% "scalajs-leaflet-markercluster"   % "0.0.0",
    Common.ORG      %%% "scalajs-leaflet-react"           % "0.0.0",

    "com.github.japgolly.scalajs-react" %%% "extra"       % Common.reactSjsVsn
)

