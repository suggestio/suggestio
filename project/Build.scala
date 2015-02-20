import sbt._
import Keys._
import play.Play.autoImport._
import PlayKeys._
import play.twirl.sbt.Import._
import play.twirl.sbt._
import com.typesafe.sbt.web._

object SiobixBuild extends Build {

  lazy val util = project

  /*lazy val cascadingEs2 = {
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
  )*/

  lazy val advExtCommon = {
    val name = "advext-common"
    Project(
      id = name,
      base = file(name)
    )
  }

  lazy val advExtSjsRunner = {
    val name = "advext-sjs-runner"
    Project(
      id = name,
      base = file(name),
      dependencies = Seq(advExtCommon)
    )
  }

  lazy val utilPlay = {
    val name = "util-play"
    Project(
      id = name,
      base = file(name),
      dependencies = Seq(util)
    )
  }

  lazy val securesocial = project
    .enablePlugins(play.PlayScala, SbtWeb)

  lazy val web21 = project
    .dependsOn(advExtCommon, util, utilPlay, securesocial)
    .enablePlugins(play.PlayScala, SbtWeb)

  lazy val root = Project(
    id = "root",
    base = file(".")
  )
  .aggregate(advExtCommon, advExtSjsRunner, util, utilPlay, securesocial, web21)

}
