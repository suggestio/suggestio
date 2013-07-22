// Comment to get more information during initialization
logLevel := Level.Warn

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "CBCA private repo" at "http://ivy2-internal.cbca.ru/sbt/"
)

resolvers += "DiversIT repo" at "http://repository-diversit.forge.cloudbees.com/release"

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "2.1.1")

addSbtPlugin("patience" % "play-stylus" % "0.1.3" from "http://ivy2-internal.cbca.ru/sbt/patience/play-stylus_2.9.2_0.12/0.1.3/play-stylus-0.1.3.jar")

addSbtPlugin("eu.diversit.sbt.plugin" % "webdav4sbt" % "1.3")
