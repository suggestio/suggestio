Common.settingsOrgJS

enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-common"

version := "0.0.0-SNAPSHOT"

persistLauncher in Compile := false

persistLauncher in Test := false

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    Common.ORG      %%% "common-sjs"                  % "0.0.0-SNAPSHOT",
    //"org.scala-js"  %%% "scalajs-dom"                 % Common.sjsDomVsn,
    "com.github.japgolly.scalajs-react" %%% "core"    % Common.reactSjsVsn
)

npmDependencies in Compile ++= Seq(
  "react"     -> Common.reactJsVsn,
  "react-dom" -> Common.reactJsVsn
)

/*
jsDependencies ++= Seq(
  // React JS itself (Note the filenames, adjust as needed, eg. to remove addons.)
  "org.webjars.bower" % "react" % Common.reactJsVsn
    /        "react-with-addons.js"
    minified "react-with-addons.min.js"
    commonJSName "React",

  "org.webjars.bower" % "react" % Common.reactJsVsn
    /         "react-dom.js"
    minified  "react-dom.min.js"
    dependsOn "react-with-addons.js"
    commonJSName "ReactDOM",

  "org.webjars.bower" % "react" % Common.reactJsVsn
    /         "react-dom-server.js"
    minified  "react-dom-server.min.js"
    dependsOn "react-dom.js"
    commonJSName "ReactDOMServer"
)
*/

