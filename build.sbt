Common.settingsOrg

name := "mbill2"

version := "0.0.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= Seq(
  "typesafe-releases" at "https://ivy2-internal.cbca.ru/artifactory/typesafe-releases",
  "sonatype-oss-releases" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-releases"
)


libraryDependencies ++= {
  Seq(
    "com.google.inject"     %  "guice"                % "4.0",
    "joda-time"             %  "joda-time"            % "2.8.+",
    Common.ORG              %% "common"               % "0.0.0-SNAPSHOT",
    Common.ORG              %% "common-slick-driver"  % Common.sioSlickDrvVsn,
    Common.ORG              %% "util"                 % "2.0.1-SNAPSHOT"
      exclude("org.jruby", "jruby-complete")
      exclude("org.slf4j", "slf4j-log4j12")
      exclude("log4j", "log4j")
      exclude("org.slf4j", "log4j-over-slf4j"),
    Common.ORG              %% "logs-macro"           % "0.0.0-SNAPSHOT"
    //"org.scalatest"       %% "scalatest"            % "2.2.+" % "test"   // Потом надо на 2.3 переключится.
  )
}

