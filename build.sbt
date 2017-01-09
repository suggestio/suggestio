Common.settingsOrg

name := "logs-macro"

version := "0.0.0-SNAPSHOT"

resolvers ++= Seq(
  "typesafe-releases" at Common.Repo.TYPESAFE_RELEASES_URL
)


libraryDependencies ++= {
  Seq(
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.+"
  )
}

//testOptions in Test += Tests.Argument("-oF")

