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

  /** Cross-buildable утиль для упрощенной сборки Enumeration-моделей. */
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

  /** Общий код серверной и клиентской частей подсистемы внешнего размещения. */
  lazy val advExtCommon = {
    val name = "advext-common"
    Project(
      id = name,
      base = file(name),
      dependencies = Seq(modelEnumUtil)
    )
  }

  /** Кое-какие общие вещи для js. */
  lazy val commonSjs = {
    val name = "common-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlay)
      // Чтобы не инклюдчить сорцы modelEnumUtil в каждом под-проекте, используем его прямо здесь.
      .settings(
        unmanagedSourceDirectories in Compile <++= unmanagedSourceDirectories in (modelEnumUtil, Compile)
      )
  }

  /** Модуль scala.js для подсистемы внешнего размещения исторически отдельно и он довольно жирный и сложный, чтобы жить внутри дерева lk-sjs. */
  lazy val lkAdvExtSjs = {
    val name = "lk-adv-ext-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlay)
      .dependsOn(commonSjs, advExtCommon)
      .settings(
        unmanagedSourceDirectories in Compile <++= unmanagedSourceDirectories in (advExtCommon, Compile)
      )
  }

  /** Все мелкие скрипты кроме выдачи (т.е. весь my.suggest.io + буклет и т.д) объеденены в одном большом js. */
  lazy val lkSjs = {
    val name = "lk-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlay)
      .dependsOn(commonSjs, lkAdvExtSjs)
  }


  /** Утиль, была когда-то расшарена между siobix и sioweb. Постепенно стала просто свалкой. */
  lazy val util = project
    .dependsOn(modelEnumUtil, advExtCommon)

  /** Внутренний форк securesocial. */
  lazy val securesocial = project
    .enablePlugins(PlayScala, SbtWeb)

  /** веб-интерфейс suggest.io v2. */
  lazy val web21 = project
    // Список sjs-проектов нельзя вынести за скобки из-за ограничений синтаксиса вызова aggregate().
    .dependsOn(advExtCommon, util, securesocial, modelEnumUtilPlay)
    .aggregate(lkSjs)
    .enablePlugins(PlayScala, SbtWeb, PlayScalaJS)
    .settings(
      scalaJSProjects := Seq(lkSjs),
      pipelineStages += scalaJSProd
    )
  


  lazy val root = {
    Project(id = "root", base = file("."))
      .settings(
        scalaVersion := "2.11.6"
      )
      .aggregate(modelEnumUtil, modelEnumUtilPlay, advExtCommon, lkAdvExtSjs, lkSjs, util, securesocial, web21)
  }

}
