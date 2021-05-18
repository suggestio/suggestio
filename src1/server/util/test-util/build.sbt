import play.sbt.PlayImport

Common.settingsOrg

name := "srv-test-util"

version := "0.0.0"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= {
  Seq(
    "com.typesafe.play" %% "play-json" % Common.Vsn.PLAY_JSON_VSN,
    "org.scalatest" %% "scalatest" % Common.scalaTestVsn
  )
}

