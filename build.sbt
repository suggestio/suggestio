import org.scalajs.core.tools.javascript.OutputMode

Common.settingsOrg

name := "lk-sjs"

version := "0.0.0-SNAPSHOT"

//resolvers ++= Seq(
//  "sonatype-oss-snapshots" at Common.Repo.SONATYPE_OSS_SNAPSHOTS_URL
//)

libraryDependencies ++= Seq(
  "be.doeraene"         %%% "scalajs-jquery"      % Common.sjsJqueryVsn,
  Common.ORG            %%% "lk-adv-ext-sjs"      % "0.0.0-SNAPSHOT",
  Common.ORG            %%% "lk-adv-direct-sjs"   % "0.0.0-SNAPSHOT",
  Common.ORG            %%% "lk-adv-geo-tags-sjs" % "0.0.0-SNAPSHOT",
  Common.ORG            %%% "lk-adn-map-sjs"      % "0.0.0-SNAPSHOT"
  //"org.monifu"  %%% "minitest" % "0.12" % "test",
)

persistLauncher in Compile := true

persistLauncher in Test := false

//testFrameworks += new TestFramework("minitest.runner.Framework")

jsDependencies += RuntimeDOM % "test"

//scalaJSOutputMode := OutputMode.ECMAScript6

// Пока не нужно, ибо не минифицировано и не версия jquery у нас более старая. Но потом надо будет это заюзать.
// skip in packageJSDependencies := true

