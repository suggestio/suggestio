//dependencyOverrides += "jline" % "jline" % "2.14.4"

// Comment to get more information during initialization
//logLevel := Level.Warn

//offline := true

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

resolvers ++= Seq(
  ("typesafe-releases"            at "http://ci.suggest.io/artifactory/typesafe-releases").withAllowInsecureProtocol(true),
  ("bintray-nitram509-jbrotli"    at "http://ci.suggest.io/artifactory/bintray-nitram509-jbrotli").withAllowInsecureProtocol(true)
)

// Для ускорения update на куче subprojects.
//addSbtPlugin("nz.co.bottech" % "sbt-cached-updates" % "1.0.+")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.2")


libraryDependencies += (
  "org.meteogroup.jbrotli" % "jbrotli-native-linux-x86-amd64" % "0.5.0"
)

//addSbtPlugin("io.suggest" % "sbt-web-brotli" % "0.5.6-SNAPSHOT")
addSbtPlugin("com.github.enalmada" % "sbt-web-brotli" % "0.5.5")


addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.3")

//addSbtPlugin("org.github.ngbinh" % "sbt-simple-url-update" % "1.0.5-SNAPSHOT")
addSbtPlugin("org.github.ngbinh" % "sbt-simple-url-update" % "1.0.4")


// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.2.2")

// stylus assets
//addSbtPlugin("com.typesafe.sbt" % "sbt-stylus" % "1.0.2s49-SNAPSHOT")
addSbtPlugin("com.typesafe.sbt" % "sbt-stylus" % "1.1.0")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.32")
//addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.0.1")

//addSbtPlugin("com.slidingautonomy.sbt" % "sbt-filter" % "1.0.1")
addSbtPlugin("com.github.praneethpuligundla" % "sbt-filter" % "1.0.2")

// Плагины для интеграции scalajs + npm + webpack.
//addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.17.0")
addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler-sjs06" % "0.17.0")

addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full)

