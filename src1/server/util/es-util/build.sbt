import play.sbt.PlayImport

Common.settingsOrg

name := "es-util"

version := "0.0.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= {
  Seq(
    "org.elasticsearch"         % "elasticsearch"       % Common.Vsn.ELASTIC_SEARCH,
    "org.elasticsearch.client"  % "transport"           % Common.Vsn.ELASTIC_SEARCH,

    "org.scalatest"             %% "scalatest"          % Common.scalaTestVsn % "test"
  )
}

//testOptions in Test += Tests.Argument("-oF")

