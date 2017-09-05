//dependencyOverrides += "jline" % "jline" % "2.14.4"

// Comment to get more information during initialization
//logLevel := Level.Warn

offline := true

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

resolvers ++= Seq(
  "typesafe-releases"       at "http://ivy2-internal.cbca.ru/artifactory/typesafe-releases"
  //"typesafe-releases"     at "http://10.0.0.254:8081/artifactory/typesafe-releases"
)

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.2")

addSbtPlugin("org.neolin.sbt" % "sbt-simple-url-update" % "1.0.0.2-SNAPSHOT")


// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.2.1")

// stylus assets
addSbtPlugin("com.typesafe.sbt" % "sbt-stylus" % "1.0.2s49-SNAPSHOT")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.20")

addSbtPlugin("com.slidingautonomy.sbt" % "sbt-filter" % "1.0.1")

// Плагины для интеграции scalajs + npm + webpack.
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.7.0")

addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.7.0")
