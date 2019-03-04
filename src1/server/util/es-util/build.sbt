import play.sbt.PlayImport

Common.settingsOrg

name := "es-util"

version := "0.0.0"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= {
  Seq(
    //"com.google.inject"         % "guice"               % Common.Vsn.GUICE,
    "com.google.inject.extensions" % "guice-assistedinject" % Common.Vsn.GUICE,

    // Походу в elasticsearch набрали совсем энтерпрайз-java-дебилов с финализированными паттернами
    // верхнего переднего межушного нервного узла.
    // Тут мы костылим фееричный log4j-wontfix-баг с 
    // См. https://github.com/elastic/elasticsearch/issues/19415
    // Используем специально-написанный для es-5.x log4j2-костыль: https://github.com/ctron/de.dentrassi.elasticsearch.log4j2-mock/
    "de.dentrassi.elasticsearch" % "log4j2-mock"        % "0.0.1",
    "org.apache.logging.log4j"   % "log4j-to-slf4j"     % Common.Vsn.LOG4J,

    "org.elasticsearch"         % "elasticsearch"       % Common.Vsn.ELASTIC_SEARCH,
    "org.elasticsearch.client"  % "transport"           % Common.Vsn.ELASTIC_SEARCH,

    "org.scalatest"             %% "scalatest"          % Common.scalaTestVsn % "test"
  )
}

//testOptions in Test += Tests.Argument("-oF")

