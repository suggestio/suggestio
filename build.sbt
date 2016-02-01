import org.scalajs.core.tools.javascript.OutputMode

Common.settingsOrg

name := "lk-sjs"

version := "0.0.0-SNAPSHOT"

resolvers ++= Seq(
  "sonatype-oss-snapshots" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-snapshots"
)

libraryDependencies ++= Seq(
  "be.doeraene"         %%% "scalajs-jquery"      % "0.8.1",
  Common.ORG            %%% "lk-adv-ext-sjs"      % "0.0.0-SNAPSHOT",
  Common.ORG            %%% "lk-adv-direct-sjs"   % "0.0.0-SNAPSHOT",
  Common.ORG            %%% "common-sjs"          % "0.0.0-SNAPSHOT",
  Common.ORG            %%% "map-rad-sjs"         % "0.0.0-SNAPSHOT"
  //"org.monifu"  %%% "minitest" % "0.12" % "test",
)

persistLauncher in Compile := true

persistLauncher in Test := false

//testFrameworks += new TestFramework("minitest.runner.Framework")

jsDependencies += RuntimeDOM % "test"

//scalaJSOutputMode := OutputMode.ECMAScript6

// Пока не нужно, ибо не минифицировано и не версия jquery у нас более старая. Но потом надо будет это заюзать.
skip in packageJSDependencies := true

