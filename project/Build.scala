import sbt._
import Keys._
import play.Play.autoImport._
import PlayKeys._
import play.twirl.sbt.Import._
import play.twirl.sbt._
import com.typesafe.sbt.web._

object SiobixBuild extends Build {

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

  lazy val modelEnumUtil = {
    val name = "model-enum-util"
    Project(
      id = name,
      base = file(name)
    )
  }

  lazy val modelEnumUtilPlay = {
    val name = "model-enum-util-play"
    Project(
      id    = name,
      base  = file(name),
      dependencies = Seq(modelEnumUtil)
    )
  }

  lazy val advExtCommon = {
    val name = "advext-common"
    Project(
      id = name,
      base = file(name),
      dependencies = Seq(modelEnumUtil)
    )
  }

  lazy val advExtSjsRunner = {
    val name = "advext-sjs-runner"
    Project(id = name, base = file(name))
      .dependsOn(advExtCommon)
      .settings(
        unmanagedSourceDirectories in Compile <++= unmanagedSourceDirectories in (advExtCommon, Compile),
        unmanagedSourceDirectories in Compile <++= unmanagedSourceDirectories in (modelEnumUtil, Compile)
      )
  }

  lazy val util = project
    .dependsOn(modelEnumUtil)

  /*lazy val utilPlay = {
    val name = "util-play"
    Project(
      id = name,
      base = file(name),
      dependencies = Seq(util)
    )
  }*/

  lazy val securesocial = project
    .enablePlugins(play.PlayScala, SbtWeb)

  lazy val web21 = project
    .dependsOn(advExtCommon, util, securesocial, modelEnumUtilPlay)
    .enablePlugins(play.PlayScala, SbtWeb)

  lazy val root = Project(
    id = "root",
    base = file(".")
  )
  .aggregate(modelEnumUtil, modelEnumUtilPlay, advExtCommon, advExtSjsRunner, util, securesocial, web21)

}
