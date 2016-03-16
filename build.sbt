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
    Common.ORG      %%% "lk-adv-common-sjs"       % "0.0.0-SNAPSHOT",
    Common.ORG      %%% "map-rad-sjs"             % "0.0.0-SNAPSHOT",
    Common.ORG      %%% "lk-tags-edit-sjs"        % "0.0.0-SNAPSHOT",
    "com.lihaoyi"   %%% "utest"                   % "0.3.1"  % "test"
)

