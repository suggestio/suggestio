// Comment to get more information during initialization
//logLevel := Level.Warn

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

resolvers ++= Seq(
  "typesafe-releases"       at "https://ivy2-internal.cbca.ru/artifactory/typesafe-releases",
  //"typesafe-snapshots"    at "https://ivy2-internal.cbca.ru/artifactory/typesafe-snapshots",
  "typesafe-snapshots"      at "http://repo.typesafe.com/typesafe/snapshots/",
  Resolver.url("bintray-repository", url("http://dl.bintray.com/neomaclin/sbt-plugins/"))(Resolver.ivyStylePatterns),
  //"sbt-snapshot-plugins"  at "https://ivy2-internal.cbca.ru/artifactory/sbt-plugin-snapshots"
  Resolver.url("sbt-snapshot-plugins", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns)
)

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.2")

//addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.6")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.0.0")

//addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.0.0")

//addSbtPlugin("com.typesafe.sbt" % "sbt-uglify" % "1.0.1")


// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.0-M2")

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.0.2")

//addSbtPlugin("com.typesafe.sbt" % "sbt-webdriver" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.0.1")

// stylus assets
addSbtPlugin("com.typesafe.sbt" % "sbt-stylus" % "1.0.2s49-SNAPSHOT")

//addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "+")

addSbtPlugin("com.tuplejump" % "sbt-yeoman" % "0.8.5-SNAPSHOT")

// ProGuard занимается обфускацией скомпиленных данных.
addSbtPlugin("com.typesafe.sbt" % "sbt-proguard" % "0.2.3-SNAPSHOT")

addSbtPlugin("org.neolin.sbt" % "sbt-simple-url-update" % "1.0.0")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.1")

addSbtPlugin("com.slidingautonomy.sbt" % "sbt-filter" % "1.0.1")

addSbtPlugin("com.vmunier" % "sbt-play-scalajs" % "0.2.3")

