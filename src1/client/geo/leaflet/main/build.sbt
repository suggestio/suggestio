Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-leaflet"

version := "0.1s-SNAPSHOT"

persistLauncher in Compile := false

persistLauncher in Test := false

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    Common.ORG      %%% "common-sjs"       % "0.0.0-SNAPSHOT",
    "org.scala-js"  %%% "scalajs-dom"      % Common.sjsDomVsn
)

npmDependencies in Compile ++= Seq(
  "leaflet"               -> Common.leafletJsVsn,
  "leaflet.locatecontrol" -> Common.leafletControlLocateJsVsn
)

requiresDOM in Test := true

/*
jsDependencies ++= {
  val leafletVsn = Common.leafletJsVsn
  val llcVsn     = Common.leafletControlLocateJsVsn
  Seq(
    // leaflet.js
    "org.webjars.bower" % "leaflet" % leafletVsn
      / s"$leafletVsn/dist/leaflet-src.js"
      minified s"$leafletVsn/dist/leaflet.js",

    // locate control TODO Вынести в отдельный пакет вместе с API.
    "org.webjars" % "leaflet-locatecontrol" % Common.leafletControlLocateWjVsn
      / s"$llcVsn/src/L.Control.Locate.js"
      minified s"$llcVsn/dist/L.Control.Locate.min.js"
      dependsOn s"$leafletVsn/dist/leaflet-src.js",

    //"org.webjars" % "leaflet"               % "0.7.7"   / "0.7.7/leaflet.js",
    //"org.webjars" % "leaflet-locatecontrol" % "0.40.0"  / "0.40.0/L.Control.Locate.js",
    RuntimeDOM    % "test"
  )
}
*/

