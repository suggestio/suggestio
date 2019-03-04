Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-asmcryptojs"

version := "0.0.0"

testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "io.monix"     %%% "minitest"    % Common.minitestVsn  % Test
)

npmDependencies in Compile ++= Seq(
  "asmcrypto.js"   -> Common.Vsn.ASM_CRYPTO_JS
)

