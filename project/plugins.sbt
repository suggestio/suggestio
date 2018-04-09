//dependencyOverrides += "jline" % "jline" % "2.14.4"

// Comment to get more information during initialization
//logLevel := Level.Warn

offline := true

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

resolvers ++= Seq(
  "typesafe-releases"            at "http://ivy2-internal.cbca.ru/artifactory/typesafe-releases",
  "bintray-nitram509-jbrotli"    at "http://ivy2-internal.cbca.ru/artifactory/bintray-nitram509-jbrotli"
)

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.2")


libraryDependencies += (
  "org.meteogroup.jbrotli" % "jbrotli-native-linux-x86-amd64" % "0.5.0"
)

addSbtPlugin("io.suggest" % "sbt-web-brotli" % "0.5.6-SNAPSHOT")


addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.3")

addSbtPlugin("org.github.ngbinh" % "sbt-simple-url-update" % "1.0.5-SNAPSHOT")


// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.12")

addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.2.2")

// stylus assets
addSbtPlugin("com.typesafe.sbt" % "sbt-stylus" % "1.0.2s49-SNAPSHOT")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.22")

addSbtPlugin("com.slidingautonomy.sbt" % "sbt-filter" % "1.0.1")

// Плагины для интеграции scalajs + npm + webpack.
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.12.0")

addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.9.0-SNAPSHOT")

