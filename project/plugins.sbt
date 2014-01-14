// Comment to get more information during initialization
logLevel := Level.Warn

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

resolvers ++= Seq(
  //"JMParsons Releases" at "http://jmparsons.github.io/releases/",
  //"DiversIT repo" at "http://repository-diversit.forge.cloudbees.com/release",
  Resolver.file("Local Repository", file("/home/user/.ivy/local"))(Resolver.ivyStylePatterns),
  "typesafe-releases"   at "http://repo.typesafe.com/typesafe/releases/",
  "typesafe-snapshots"  at "http://repo.typesafe.com/typesafe/snapshots/",
  Resolver.url("sbt-snapshot-plugins", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns),
  "cbca-repo"           at "http://ivy2-internal.cbca.ru/sbt/"
)

// Comment it out, on non-snapshot releases
addSbtPlugin("com.typesafe" % "sbt-web" % "1.0.0-SNAPSHOT")

addSbtPlugin("com.typesafe" % "sbt-webdriver" % "1.0.0-SNAPSHOT")

addSbtPlugin("com.typesafe" % "sbt-js-engine" % "1.0.0-SNAPSHOT")


// Use the Play sbt plugin for Play projects
//addSbtPlugin("play" % "sbt-plugin" % "2.1.3")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3-SNAPSHOT")

// stylus assets
addSbtPlugin("patience" % "play-stylus" % "0.2.0")

//addSbtPlugin("eu.diversit.sbt.plugin" % "webdav4sbt" % "1.3")

// play 2.1.x: LESS format problems:
//addSbtPlugin("com.jmparsons" % "play-lessc" % "0.0.8")

