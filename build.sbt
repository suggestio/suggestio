Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

name := "lk-adn-map-sjs"

version := "0.0.0-SNAPSHOT"

persistLauncher in Compile := false

persistLauncher in Test := false

//testFrameworks += new TestFramework("utest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG          %%% "lk-adv-common-sjs"   % "0.0.0-SNAPSHOT",
  Common.ORG          %%% "lk-dt-period-sjs"    % "0.0.0-SNAPSHOT",
  //Common.ORG          %%% "scalajs-leaflet"     % "0.1s-SNAPSHOT",
  Common.ORG          %%% "maps-sjs"            % "0.0.0-SNAPSHOT",
  //Common.ORG        %%% "scalajs-leaflet"     % "0.1s-SNAPSHOT"
  //"com.lihaoyi"     %%% "utest"               % "0.3.1"  % "test"
  "org.scala-js"      %%% "scalajs-dom"         % Common.sjsDomVsn
)

jsDependencies ++= Seq(
  // Стандартные jsDeps:
  RuntimeDOM % "test"
)

