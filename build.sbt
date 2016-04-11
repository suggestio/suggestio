Common.settingsOrg

name := "common-sjs"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.scala-js"        %%% "scalajs-dom"         % Common.sjsDomVsn,
  Common.ORG            %%  "common"              % "0.0.0-SNAPSHOT",
  "be.doeraene"         %%% "scalajs-jquery"      % Common.sjsJqueryVsn,
  "org.monifu"          %%% "minitest"            % "0.12"            % "test"
)

persistLauncher in Compile := false

persistLauncher in Test := false

testFrameworks += new TestFramework("minitest.runner.Framework")

jsDependencies += RuntimeDOM % "test"

