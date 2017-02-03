import play.sbt.PlayImport

Common.settingsOrg

name := "srv-test-util"

version := "0.0.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= {
  Seq(
    PlayImport.json,
    //"org.slf4j" % "slf4j-api" % Common.Vsn.SLF4J,
    //"org.slf4j" % "slf4j-log4j12" % Common.Vsn.SLF4J,
    "org.scalatest" %% "scalatest" % Common.scalaTestVsn
  )
}

