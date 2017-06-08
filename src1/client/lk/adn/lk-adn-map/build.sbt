Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

name := "lk-adn-map-sjs"

version := "0.0.0-SNAPSHOT"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG          %%% "lk-adv-common-sjs"   % "0.0.0-SNAPSHOT",
  Common.ORG          %%% "lk-dt-period-sjs"    % "0.0.0-SNAPSHOT",
  Common.ORG          %%% "maps-sjs"            % "0.0.0-SNAPSHOT",
  "org.scala-js"      %%% "scalajs-dom"         % Common.sjsDomVsn
)

