Common.settingsOrgJS

//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-leaflet-react"

version := "0.0.0"

// Show more comments when using dubious features
scalacOptions += "-feature"


libraryDependencies ++= Seq(
    Common.ORG      %%% "common-sjs"                     % "0.0.0",
    Common.ORG      %%% "scalajs-leaflet"                % "0.1s",
    Common.ORG      %%% "scalajs-leaflet-markercluster"  % "0.0.0",
    Common.ORG      %%% "scalajs-react-common"           % "0.0.0"
)

npmDependencies in Compile ++= Seq(
  "react-leaflet" -> Common.Vsn.REACT_LEAFLET
)

