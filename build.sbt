Common.settingsOrgJS

enablePlugins(ScalaJSPlugin)

name := "scalajs-leaflet-react"

version := "0.0.0-SNAPSHOT"

persistLauncher in Compile := false

persistLauncher in Test := false

//testFrameworks += new TestFramework("utest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"


libraryDependencies ++= Seq(
    Common.ORG      %%% "common-sjs"                     % "0.0.0-SNAPSHOT",
    Common.ORG      %%% "scalajs-leaflet"                % "0.1s-SNAPSHOT",
    Common.ORG      %%% "scalajs-leaflet-markercluster"  % "0.0.0-SNAPSHOT",
    Common.ORG      %%% "scalajs-react-common"           % "0.0.0-SNAPSHOT"
)

jsDependencies ++= Seq(
  // Зависим от ванильного react-leaflet:
  "org.webjars.bower" % "react-leaflet" % "1.0.1"
    / "react-leaflet.js"
    minified "react-leaflet.min.js"
)

