import play.sbt.PlayImport

Common.settingsOrg

name := "es-util"

version := "0.0.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= {
  Seq(
    "org.elasticsearch"       % "elasticsearch"     % Common.Vsn.ELASTIC_SEARCH,
    "com.sksamuel.elastic4s" %% "elastic4s-core"    % Common.Vsn.ELASTIC4S,
    "com.sksamuel.elastic4s" %% "elastic4s-streams" % Common.Vsn.ELASTIC4S,

    "org.scalatest"          %% "scalatest"         % Common.scalaTestVsn % "test"
  )
}

//testOptions in Test += Tests.Argument("-oF")

