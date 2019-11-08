Common.settingsOrg

name := "logs-macro"

version := "0.0.0"

resolvers ++= Seq(
  ("typesafe-releases" at Common.Repo.TYPESAFE_RELEASES_URL).withAllowInsecureProtocol(true)
)


libraryDependencies ++= {
  Seq(
    //"com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.+"
    "com.typesafe.scala-logging" %% "scala-logging" % "3.+"
  )
}

//testOptions in Test += Tests.Argument("-oF")

