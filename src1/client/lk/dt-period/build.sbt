Common.settingsOrgJS

//enablePlugins(ScalaJSPlugin)
enablePlugins(ScalaJSBundlerPlugin)

name := "lk-dt-period-sjs"

version := "0.0.0"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    "org.scala-js"  %%% "scalajs-dom"                     % Common.sjsDomVsn,
    "io.github.cquiroz" %%% "scala-java-time-tzdb"        % Common.Vsn.SCALA_JAVA_TIME,
)

useYarn := true
