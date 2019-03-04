Common.settingsOrg

name := "streams-util"

version := "0.0.0"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= {
  Seq(
    // akka-streams
    "com.typesafe.akka" %% "akka-stream" % Common.Vsn.AKKA,

    "javax.inject" % "javax.inject" % "1",

    // test
    "org.scalatest" %% "scalatest" % Common.scalaTestVsn % Test
  )
}

