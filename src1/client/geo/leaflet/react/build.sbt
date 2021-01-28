Common.settingsOrgJS

//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-leaflet-react"

version := Common.Vsn.REACT_LEAFLET

// Show more comments when using dubious features
scalacOptions += "-feature"

npmDependencies in Compile ++= Seq(
  "react-leaflet" -> Common.Vsn.REACT_LEAFLET
)

useYarn := true
