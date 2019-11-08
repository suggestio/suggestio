Common.settingsOrg

name := "common-slick-driver"

version := "1.2.0"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= {
  Seq(
    "com.google.inject"     %  "guice"                % Common.Vsn.GUICE,
    // TODO Fix для ошибки sbt-0.13.13: impossible to get artifacts when data has not been loaded. IvyNode = org.scala-lang#scala-reflect;2.11.8
    "org.scala-lang"        % "scala-reflect"         % Common.SCALA_VSN,
    //"joda-time"           %  "joda-time"            % "2.8.+",
    "com.typesafe.slick"    %% "slick"                % Common.Vsn.SLICK,
    "com.github.tminglei"   %% "slick-pg"             % Common.Vsn.SLICK_PG
    //"com.github.tminglei" %% "slick-pg_joda-time"   % Common.Vsn.SLICK_PG
  )
}

