import play.sbt.PlayImport

Common.settingsOrg

name := "es-util"

version := "0.0.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= {
  Seq(
    //"com.google.inject"         % "guice"               % Common.Vsn.GUICE,
    "com.google.inject.extensions" % "guice-assistedinject" % Common.Vsn.GUICE,

    "org.elasticsearch"         % "elasticsearch"       % Common.Vsn.ELASTIC_SEARCH,
    "org.elasticsearch.client"  % "transport"           % Common.Vsn.ELASTIC_SEARCH,

    "org.scalatest"             %% "scalatest"          % Common.scalaTestVsn % "test"
  )
}

//testOptions in Test += Tests.Argument("-oF")

