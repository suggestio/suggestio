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

// Comment it out, on non-snapshot releases
addSbtPlugin("com.typesafe" % "sbt-web" % "1.0.0-SNAPSHOT")

addSbtPlugin("com.typesafe" % "sbt-webdriver" % "1.0.0-SNAPSHOT")

addSbtPlugin("com.typesafe" % "sbt-js-engine" % "1.0.0-SNAPSHOT")


// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3-SNAPSHOT")

// stylus assets
addSbtPlugin("patience" % "play-stylus" % "1.0.0-SNAPSHOT")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "+")

