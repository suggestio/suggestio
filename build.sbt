name := "logs-macro"

organization := "io.suggest"

version := "0.0.0-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers ++= Seq(
  "typesafe-releases" at "https://ivy2-internal.cbca.ru/artifactory/typesafe-releases"
)


libraryDependencies ++= {
  Seq(
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.+"
  )
}

//testOptions in Test += Tests.Argument("-oF")

