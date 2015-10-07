name := "swfs"

organization := "io.suggest"

version := "0.0.0-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers ++= Seq(
  "typesafe-releases" at "https://ivy2-internal.cbca.ru/artifactory/typesafe-releases",
  "sonatype-oss-releases" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-releases",
  "apache-releases" at "https://ivy2-internal.cbca.ru/artifactory/apache-releases",
  "conjars-repo" at "https://ivy2-internal.cbca.ru/artifactory/conjars-repo",
  "maven-twttr-com" at "https://ivy2-internal.cbca.ru/artifactory/maven-twttr-com",
  "sonatype-oss-snapshots" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-snapshots",
  "websudos-releases" at "https://ivy2-internal.cbca.ru/artifactory/websudos-local-releases"
)


libraryDependencies ++= {
  val playVsn       = "2.4.3"
  Seq(
    "io.suggest"        %% "common"       % "0.0.0-SNAPSHOT",
    "io.suggest"        %% "common-play"  % "0.0.0-SNAPSHOT",
    "com.typesafe.play" %% "play-json"    % playVsn,
    "com.typesafe.play" %% "play-ws"      % playVsn,
    // test
    "org.scalatest"     %% "scalatest" % "2.2.+" % "test"   // Потом надо на 2.3 переключится.
  )
}

//testOptions in Test += Tests.Argument("-oF")

