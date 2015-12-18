name := "n2"

organization := "io.suggest"

version := "0.0.0-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers ++= Seq(
  "typesafe-releases" at "https://ivy2-internal.cbca.ru/artifactory/typesafe-releases",
  "sonatype-oss-releases" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-releases",
  "sonatype-oss-snapshots" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-snapshots"
)


libraryDependencies ++= {
  val playVsn       = "2.4.6"
  Seq(
    "io.suggest"        %% "util" % "2.0.1-SNAPSHOT" changing()
      exclude("org.jruby", "jruby-complete")
      exclude("org.slf4j", "slf4j-log4j12")
      exclude("log4j", "log4j")
      exclude("org.slf4j", "log4j-over-slf4j"),
    "io.suggest"        %% "swfs" % "0.0.0-SNAPSHOT"
      exclude("org.jruby", "jruby-complete")
      exclude("org.slf4j", "slf4j-log4j12")
      exclude("log4j",     "log4j")
      exclude("org.slf4j", "log4j-over-slf4j"),
    // test
    "org.scalatest"     %% "scalatest" % "2.2.+" % "test",
    "org.scalatestplus" %% "play" % "1.4.0-SNAPSHOT" % "test"
      exclude("commons-logging", "commons-logging")
      exclude("org.w3c.css", "sac")
  )
}

//testOptions in Test += Tests.Argument("-oF")

// В тестах используется EhCache, который отваливается между тестами.
// http://stackoverflow.com/a/32180497
parallelExecution in Test := false

