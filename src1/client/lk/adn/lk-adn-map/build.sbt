Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)
enablePlugins(ScalaJSBundlerPlugin)

name := "lk-adn-map-sjs"

version := "0.0.0"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG          %%% "lk-adv-common-sjs"   % "0.0.0",
  Common.ORG          %%% "lk-dt-period-sjs"    % "0.0.0",
  Common.ORG          %%% "maps-sjs"            % "0.0.0",
  "org.scala-js"      %%% "scalajs-dom"         % Common.sjsDomVsn
)

useYarn := true
