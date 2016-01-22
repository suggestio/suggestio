Common.settingsOrg

name := "lk-adv-ext-sjs"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  Common.ORG      %% "common"           % "0.0.0-SNAPSHOT",
  "be.doeraene"   %%% "scalajs-jquery"  % "0.8.1",
  Common.ORG      %%% "common-sjs"      % "0.0.0-SNAPSHOT",
  //"com.lihaoyi"   %%% "upickle"       % "0.2.+",
  "org.monifu"    %%% "minitest"        % "0.12" % "test"
)

//persistLauncher in Compile := true

persistLauncher in Test := false

testFrameworks += new TestFramework("minitest.runner.Framework")

// требуется DOM в тестах. http://www.scala-js.org/doc/tutorial.html
jsDependencies += RuntimeDOM

