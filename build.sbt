name := "swfs"

organization := "io.suggest"

version := "0.0.0-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers ++= Seq(
  "typesafe-releases" at "https://ivy2-internal.cbca.ru/artifactory/typesafe-releases",
  "sonatype-oss-releases" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-releases",
  "sonatype-oss-snapshots" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-snapshots"
)


libraryDependencies ++= {
  val playVsn       = "2.4.3"
  Seq(
    "io.suggest"        %% "common"       % "0.0.0-SNAPSHOT",
    "io.suggest"        %% "common-play"  % "0.0.0-SNAPSHOT",
    "io.suggest"        %% "logs-macro"   % "0.0.0-SNAPSHOT",
    "com.typesafe.play" %% "play-json"    % playVsn,
    "com.typesafe.play" %% "play-ws"      % playVsn,
    // test
    "org.scalatest"     %% "scalatest" % "2.2.+" % "test",   // Потом надо на 2.3 переключится.
    "org.scalatestplus" %% "play" % "1.4.0-SNAPSHOT" % "test"
      exclude("commons-logging", "commons-logging")
      exclude("org.w3c.css", "sac")
  )
}

//testOptions in Test += Tests.Argument("-oF")

