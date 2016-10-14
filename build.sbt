Common.settingsOrg

name := "common"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.monifu"    %% "minitest"            % "0.12"            % "test"
)

testFrameworks += new TestFramework("minitest.runner.Framework")

