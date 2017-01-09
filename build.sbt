Common.settingsOrg

//name := "common"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "io.monix"      %%% "minitest"            % Common.minitestVsn  % "test"
)

testFrameworks += new TestFramework("minitest.runner.Framework")

