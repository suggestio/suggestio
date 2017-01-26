Common.settingsOrg

name := "mbill2"

version := "0.0.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= {
  import Common.Repo._
  Seq(
    "typesafe-releases"     at TYPESAFE_RELEASES_URL,
    "sonatype-oss-releases" at SONATYPE_OSS_RELEASES_URL
  )
}


libraryDependencies ++= {
  Seq(
    "com.google.inject"     %  "guice"                % Common.Vsn.GUICE,
    //"joda-time"             %  "joda-time"            % "2.8.+",
    //Common.ORG              %% "commonJVM"            % "0.0.0-SNAPSHOT",
    Common.ORG              %% "common-slick-driver"  % Common.Vsn.Sio.COMMON_SLICK_DRIVER,
    // slick повторно инклюдится здесь, т.к. что-то свежая версия не цеплялась через common-slick-driver
    "com.typesafe.slick"    %% "slick"                % Common.Vsn.SLICK,
    Common.ORG              %% "util"                 % "2.0.1-SNAPSHOT"
      exclude("org.jruby", "jruby-complete")
      exclude("org.slf4j", "slf4j-log4j12")
      exclude("log4j", "log4j")
      exclude("org.slf4j", "log4j-over-slf4j"),
    Common.ORG              %% "logs-macro"           % "0.0.0-SNAPSHOT"
    //"org.scalatest"       %% "scalatest"            % Common.scalaTestVsn % "test"
  )
}

