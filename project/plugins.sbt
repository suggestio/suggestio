// Comment to get more information during initialization
logLevel := Level.Warn

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

resolvers ++= Seq(
  //"JMParsons Releases" at "http://jmparsons.github.io/releases/",
  //"DiversIT repo" at "http://repository-diversit.forge.cloudbees.com/release",
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "CBCA private repo" at "http://ivy2-internal.cbca.ru/sbt/"
)

// Use the Play sbt plugin for Play projects
//addSbtPlugin("play" % "sbt-plugin" % "2.1.3")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.0")

// stylus assets
addSbtPlugin("patience" % "play-stylus" % "0.2.0")

//addSbtPlugin("eu.diversit.sbt.plugin" % "webdav4sbt" % "1.3")

// play 2.1.x: LESS format problems:
//addSbtPlugin("com.jmparsons" % "play-lessc" % "0.0.8")

