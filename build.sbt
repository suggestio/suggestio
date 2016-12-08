Common.settingsOrg

enablePlugins(ScalaJSPlugin)

name := "lk-adv-geo-tags-sjs"

version := "0.0.0-SNAPSHOT"

persistLauncher in Compile := false

persistLauncher in Test := false

testFrameworks += new TestFramework("utest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    Common.ORG      %%% "lk-adv-common-sjs"               % "0.0.0-SNAPSHOT",
    Common.ORG      %%% "map-rad-sjs"                     % "0.0.0-SNAPSHOT",
    Common.ORG      %%% "lk-tags-edit-sjs"                % "0.0.0-SNAPSHOT",
    Common.ORG      %%% "scalajs-leaflet-markercluster"   % "0.0.0-SNAPSHOT",
    Common.ORG      %%% "scalajs-leaflet-react"           % "0.0.0-SNAPSHOT",
    "com.github.japgolly.scalajs-react" %%% "core"        % Common.reactSjsVsn,
    "com.lihaoyi"   %%% "utest"                           % "0.3.1"  % "test"
)

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

