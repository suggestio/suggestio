Common.settingsOrg

name := "mbill2"

version := "0.0.0"

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= {
  import Common.Repo._
  Seq(
    ("sonatype-oss-releases" at SONATYPE_OSS_RELEASES_URL)
  )
}


libraryDependencies ++= {
  Seq(
    "com.google.inject"     %  "guice"                % Common.Vsn.GUICE,
    "org.threeten"          % "threeten-extra"        % Common.Vsn.THREETEN_EXTRA,
    // slick повторно инклюдится здесь, т.к. что-то свежая версия не цеплялась через common-slick-driver
    "com.typesafe.slick"    %% "slick"                % Common.Vsn.SLICK,
    Common.ORG              %% "util"                 % "2.0.1"
      exclude("org.jruby", "jruby-complete")
      exclude("org.slf4j", "slf4j-log4j12")
      exclude("log4j", "log4j")
      exclude("org.slf4j", "log4j-over-slf4j")
    //"org.scalatest"       %% "scalatest"            % Common.scalaTestVsn % "test"
  )
}

