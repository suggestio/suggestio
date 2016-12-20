Common.settingsOrgJS

name := "maps-sjs"

version := "0.0.0-SNAPSHOT"

//resolvers ++= Seq(
//  "sonatype-oss-snapshots" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-snapshots"
//)

libraryDependencies ++= Seq(
  Common.ORG          %%% "common-sjs"          % "0.0.0-SNAPSHOT",
  Common.ORG          %%% "scalajs-leaflet"     % "0.1s-SNAPSHOT"
)


persistLauncher in Compile := false

persistLauncher in Test := false

//testFrameworks += new TestFramework("minitest.runner.Framework")

jsDependencies += RuntimeDOM % "test"

