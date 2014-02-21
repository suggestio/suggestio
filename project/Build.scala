import sbt._
import Keys._

import java.io.{PrintWriter, File}

object SiobixBuild extends Build {

  val org = "io.suggest"

  val commonSettings = Project.defaultSettings ++ Seq(
    organization := org
  )


  lazy val util = Project(
    id = "util",
    base = file("bixo/util"),
    settings = commonSettings
  )

  lazy val cascadingEs2 = {
    val ces2 ="cascading-elasticsearch2"
    Project(
      id    = ces2,
      base  = file("bixo/" + ces2),
      dependencies = Seq(util),
      settings = commonSettings
    )
  }

  lazy val siobix = Project(
    id = "siobix",
    base = file("bixo"),
    dependencies = Seq(util, cascadingEs2),
    settings = commonSettings
  )

  lazy val web21 = Project(
    id = "web21",
    base = file("web21"),
    dependencies = Seq(util),
    settings = commonSettings
  )

  lazy val root = Project(
    id = "root",
    base = file("."),
    settings = commonSettings
  )
  .aggregate(util, cascadingEs2, siobix)

  // TODO Добавить сюда util-play?
}
