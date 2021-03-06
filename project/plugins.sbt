//logLevel := Level.Warn

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

resolvers := Seq(
  Resolver.url("sbt-plugin-releases-art",    url("https://dl.bintray.com/sbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns),
  ("bintray-nitram509-jbrotli"               at "http://ci.suggest.io/artifactory/bintray-nitram509-jbrotli").withAllowInsecureProtocol(true),
)

// Для ускорения update на куче subprojects.
//addSbtPlugin("nz.co.bottech" % "sbt-cached-updates" % "1.0.+")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.+")

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
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.8")

addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.2.2")

// stylus assets
//addSbtPlugin("com.typesafe.sbt" % "sbt-stylus" % "1.0.2s49-SNAPSHOT")
addSbtPlugin("com.typesafe.sbt" % "sbt-stylus" % "1.1.0")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.7.1")

//addSbtPlugin("com.slidingautonomy.sbt" % "sbt-filter" % "1.0.1")
addSbtPlugin("com.github.praneethpuligundla" % "sbt-filter" % "1.0.2")

// scalacenter/scalajs-bundler looks abandoned.
// 0.21.x from https://github.com/giabao/scalajs-bundler is NEEDED, because
// - @react-leaflet/core uses new module syntax with '??' operator,
//   that needs fresh WebPack 5.5x or later: https://github.com/PaulLeCam/react-leaflet/issues/891
addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.21.0-RC1-0-20211119")

addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.1" cross CrossVersion.full)

// для for-yield-оптимизации (https://github.com/fosskers/scalaz-and-cats#i-chain-operations-with-for--yield-isnt-that-all-i-need)
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
