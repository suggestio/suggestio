Common.settingsOrg

name := "brotli-util"

version := "0.0.0"

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= Seq(
  ("jbrotli" at Common.Repo.JBROTLI).withAllowInsecureProtocol(true)
)

libraryDependencies ++= {
  Seq(
    // jbrotli
    "org.meteogroup.jbrotli" % "jbrotli" % Common.Vsn.JBROTLI,
    "org.meteogroup.jbrotli" % "jbrotli-native-linux-x86-amd64" % Common.Vsn.JBROTLI,

    // akka-streams
    "com.typesafe.akka" %% "akka-stream" % Common.Vsn.AKKA,

    // test
    "org.scalatest" %% "scalatest" % Common.scalaTestVsn % Test
  )
}

