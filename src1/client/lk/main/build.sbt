//import org.scalajs.core.tools.javascript.OutputMode

Common.settingsOrgJS

name := "lk-sjs"

version := "0.0.0-SNAPSHOT"

enablePlugins(ScalaJSBundlerPlugin)

//resolvers ++= Seq(
//  "sonatype-oss-snapshots" at Common.Repo.SONATYPE_OSS_SNAPSHOTS_URL
//)

libraryDependencies ++= Seq(
  "be.doeraene"         %%% "scalajs-jquery"      % Common.sjsJqueryVsn,
  Common.ORG            %%% "lk-adv-ext-sjs"      % "0.0.0-SNAPSHOT",
  Common.ORG            %%% "lk-adv-direct-sjs"   % "0.0.0-SNAPSHOT",
  Common.ORG            %%% "lk-adv-geo-tags-sjs" % "0.0.0-SNAPSHOT",
  Common.ORG            %%% "lk-adn-map-sjs"      % "0.0.0-SNAPSHOT"
  //"io.monix"      %%% "minitest"            % Common.minitestVsn  % Test
)

persistLauncher in Compile := false

persistLauncher in Test := false

//testFrameworks += new TestFramework("minitest.runner.Framework")

requiresDOM in Test := true

//scalaJSOutputMode := OutputMode.ECMAScript6

// Пока не нужно, ибо не минифицировано и не версия jquery у нас более старая. Но потом надо будет это заюзать.
// skip in packageJSDependencies := true

