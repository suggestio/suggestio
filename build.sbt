Common.settingsOrg

enablePlugins(ScalaJSPlugin)

name := "lk-common-sjs"

version := "0.0.0-SNAPSHOT"

persistLauncher in Compile := false

persistLauncher in Test := false

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    Common.ORG      %%% "common-sjs"                  % "0.0.0-SNAPSHOT",
    "org.scala-js"  %%% "scalajs-dom"                 % Common.sjsDomVsn,
    Common.ORG      %%% "scalajs-react-common"        % "0.0.0-SNAPSHOT"
)

