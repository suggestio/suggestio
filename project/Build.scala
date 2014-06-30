import sbt._
import Keys._
import play.Play.autoImport._
import PlayKeys._
import play.twirl.sbt.Import._
import play.twirl.sbt._
import com.typesafe.sbt.web._
import java.io.{PrintWriter, File}

object SiobixBuild extends Build {

  lazy val util = Project(
    id = "util",
    base = file("bixo/util")
  )

  lazy val cascadingEs2 = {
    val ces2 ="cascading-elasticsearch2"
    Project(
      id    = ces2,
      base  = file("bixo/" + ces2),
      dependencies = Seq(util)
    )
  }

  lazy val siobix = Project(
    id = "siobix",
    base = file("bixo"),
    dependencies = Seq(util, cascadingEs2)
  )

  lazy val utilPlay = Project(
    id = "util-play",
    base = file("util-play"),
    dependencies = Seq(util)
  )

  lazy val web21 = Project(
    id = "web21",
    base = file("web21"),
    dependencies = Seq(util, utilPlay)
  )
  .enablePlugins(play.PlayScala, SbtWeb)


  lazy val root = Project(
    id = "root",
    base = file(".")
  )
  .aggregate(util, cascadingEs2, siobix, web21, web21)

}
