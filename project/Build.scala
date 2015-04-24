import play.PlayScala
import playscalajs.{PlayScalaJS, ScalaJSPlay}
import sbt._
import Keys._
import com.typesafe.sbt.web._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import com.typesafe.sbt.SbtNativePackager._
import PlayScalaJS.autoImport._

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

  lazy val sjsCommon = {
    val name = "sjs-common"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlay)
  }

  /** scala-js для формы внешнего размещения карточек. */
  lazy val advExtSjsForm = {
    val name = "advext-sjs-form"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlay)
      .dependsOn(sjsCommon, advExtCommon)
      .settings(
        unmanagedSourceDirectories in Compile <++= unmanagedSourceDirectories in (advExtCommon, Compile)
      )
  }
  
  lazy val advExtSjsRunner = {
    val name = "advext-sjs-runner"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlay)
      .dependsOn(sjsCommon, advExtCommon)
      .settings(
        Seq(advExtCommon, modelEnumUtil)
          .map(p => unmanagedSourceDirectories in Compile <++= unmanagedSourceDirectories in (p, Compile))  : _*
      )
  }

  /** Все мелкие скрипты кроме выдачи (т.е. весь my.suggest.io + буклет и т.д) теперь живут в одном большом sjs fat js. */
  lazy val lkSjs = {
    val name = "lk-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlay)
      .dependsOn(sjsCommon, advExtSjsForm, advExtSjsRunner)
  }


  lazy val util = project
    .dependsOn(modelEnumUtil, advExtCommon)

  /*lazy val utilPlay = {
    val name = "util-play"
    Project(
      id = name,
      base = file(name),
      dependencies = Seq(util)
    )
  }*/

  lazy val securesocial = project
    .enablePlugins(PlayScala, SbtWeb)

  /** веб-интерфейс suggest.io v2. */
  lazy val web21 = project
    // Список sjs-проектов нельзя вынести за скобки из-за ограничений синтаксиса вызова aggregate().
    .dependsOn(advExtCommon, util, securesocial, modelEnumUtilPlay)
    .aggregate(advExtSjsRunner, advExtSjsForm, lkSjs)
    .enablePlugins(PlayScala, SbtWeb, PlayScalaJS)
    .settings(
      scalaJSProjects := Seq(advExtSjsRunner, advExtSjsForm, lkSjs),
      pipelineStages += scalaJSProd
    )
  


  lazy val root = Project(
    id = "root",
    base = file(".")
  )
  .settings(
    scalaVersion := "2.11.6"
  )
  .aggregate(modelEnumUtil, modelEnumUtilPlay, advExtCommon, advExtSjsRunner, advExtSjsForm, lkSjs, util, securesocial, web21)



  val copySourceMapsTask = Def.task {
    val scalaFiles = (Seq(advExtCommon.base, advExtSjsRunner.base) ** ("*.scala")).get
    for (scalaFile <- scalaFiles) {
      val target = new File((classDirectory in Compile).value, scalaFile.getPath)
      IO.copyFile(scalaFile, target)
    }
  }

}
