Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-date-picker"

version := "0.39.0-SNAPSHOT"

persistLauncher in Compile := false

persistLauncher in Test := false

testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG      %%% "scalajs-react-common"  % "0.0.0-SNAPSHOT",
  "io.monix"      %% "minitest" % "0.27" % "test"
)

npmDependencies in Compile ++= Seq(
  //"react-onclickoutside" -> "5.7.1",
  "react" -> Common.reactJsVsn,
  "react-datepicker" -> "0.39.0"
)

//requiresDOM in Test := true

/*
jsDependencies ++= {
  Seq(
    "org.webjars.npm" % "react-onclickoutside" % "5.7.1"
    / "react-onclickoutside/5.7.1/index.js",

    "org.webjars.npm" % "react-datepicker" % "0.39.0"
      / "react-datepicker.js"
      minified "react-datepicker.min.js"
      dependsOn "react-with-addons.js"
      dependsOn "react-dom.js"
      dependsOn "react-onclickoutside/5.7.1/index.js"
    ,
    
    RuntimeDOM % "test"
  )
}
*/

