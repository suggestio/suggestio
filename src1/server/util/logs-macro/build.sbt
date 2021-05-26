Common.settingsOrg

name := "logs-macro"

version := "0.0.0"

libraryDependencies ++= {
  Seq(
    //"com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.+"
    "com.typesafe.scala-logging" %% "scala-logging" % "3.+"
  )
}

//testOptions in Test += Tests.Argument("-oF")

