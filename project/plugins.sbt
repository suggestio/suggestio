// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers ++= Seq(
  //"Patience Releases" at "http://repo.patience.io/"
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "2.1.1")

// add stylus css compiler for play 2.0
addSbtPlugin("patience" % "play-stylus" % "0.1.3" from "file:///home/user/.ivy2/local/patience/play-stylus/scala_2.9.3/sbt_0.12/0.1.3/jars/play-stylus.jar")

