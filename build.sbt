Common.settingsOrg

enablePlugins(ScalaJSPlugin)

name := "scalajs-leaflet-react"

version := "0.0.0-SNAPSHOT"

persistLauncher in Compile := false

persistLauncher in Test := false

//testFrameworks += new TestFramework("utest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"


libraryDependencies ++= Seq(
    Common.ORG      %%% "common-sjs"                  % "0.0.0-SNAPSHOT",
    Common.ORG      %%% "scalajs-leaflet"             % "0.1s-SNAPSHOT",
    //"org.scala-js"  %%% "scalajs-dom"               % Common.sjsDomVsn,
    "com.github.japgolly.scalajs-react" %%% "core"    % Common.reactSjsVsn
)

jsDependencies ++= Seq(
  // react-leaflet.
  // TODO Будет доступен 8 декабря 2016 после ~15-00
  "org.webjars.bower" % "react-leaflet" % "1.0.1"
    / "react-leaflet.js"
    minified "react-leaflet.min.js",

  // React JS itself (Note the filenames, adjust as needed, eg. to remove addons.)
  "org.webjars.bower" % "react" % Common.reactJsVsn
    /        "react-with-addons.js"
    minified "react-with-addons.min.js"
    commonJSName "React",

  "org.webjars.bower" % "react" % Common.reactJsVsn
    /         "react-dom.js"
    minified  "react-dom.min.js"
    dependsOn "react-with-addons.js"
    commonJSName "ReactDOM"

  // А нужно ли?
  /*
  "org.webjars.bower" % "react" % Common.reactJsVsn
    /         "react-dom-server.js"
    minified  "react-dom-server.min.js"
    dependsOn "react-dom.js"
    commonJSName "ReactDOMServer"
  */
)

