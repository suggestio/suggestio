import org.scalajs.jsenv.nodejs.NodeJSEnv

Common.settingsOrgJS

enablePlugins( ScalaJSBundlerPlugin )
// sjs-bundler: This comment start some thinks:
//  > @sjrd: If you target the Node.js runtime, you shouldn't even bundle. Node.js can load the non-bundled CommonJS module in the regular fastopt.js.
//  > https://github.com/scalacenter/scalajs-bundler/issues/250#issuecomment-395658194

libraryDependencies ++= Seq(
 "com.github.japgolly.scala-graal" %%% "core-js"       % Common.Vsn.SCALA_GRAAL,
 "com.github.japgolly.scala-graal" %%% "ext-boopickle" % Common.Vsn.SCALA_GRAAL,
)

scalaJSLinkerConfig ~= { _.withSourceMap(false) }

webpackBundlingMode := BundlingMode.LibraryOnly()

webpackConfigFile in fastOptJS := Some(baseDirectory.value / "webpack.config.js")

webpackConfigFile in fullOptJS := Some(baseDirectory.value / "webpack.config.js")

useYarn := true

jsEnv := new NodeJSEnv(
  NodeJSEnv.Config()
    .withArgs( "--max-old-space-size=4096" :: Nil )
)
