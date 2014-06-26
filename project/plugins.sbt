// Comment to get more information during initialization
//logLevel := Level.Warn

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

resolvers ++= Seq(
  "typesafe-releases"       at "https://ivy2-internal.cbca.ru/artifactory/typesafe-releases",
  //"typesafe-snapshots"    at "https://ivy2-internal.cbca.ru/artifactory/typesafe-snapshots",
  "typesafe-snapshots"      at "http://repo.typesafe.com/typesafe/snapshots/",
  //"sbt-snapshot-plugins"  at "https://ivy2-internal.cbca.ru/artifactory/sbt-plugin-snapshots"
  Resolver.url("sbt-snapshot-plugins", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns)
)

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.0.0")


// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-webdriver" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.0.0")

// stylus assets
//addSbtPlugin("patience" % "play-stylus" % "1.1.0-SNAPSHOT")
addSbtPlugin("com.typesafe.sbt" % "sbt-stylus" % "1.0.1")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "+")
