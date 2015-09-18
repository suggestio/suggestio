import play.PlayScala
import playscalajs.{PlayScalaJS, ScalaJSPlay}
import sbt._
import Keys._
import com.typesafe.sbt.web._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import org.scalajs.sbtplugin._, ScalaJSPlugin.autoImport._
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

  //}

  lazy val commonPlay = {
    val name = "common-play"
    Project(
      id    = name,
      base  = file(name),
      dependencies = Seq(common)
    )
  }

  /** Общий код серверной и клиентской частей подсистемы внешнего размещения. */
  lazy val common = {
    val name = "common"
    Project(
      id = name,
      base = file(name)
    )
  }

  /** Кое-какие общие вещи для js. */
  lazy val commonSjs = {
    val name = "common-sjs"
    Project(id = name, base = file(name))
      .dependsOn(common)
      .enablePlugins(ScalaJSPlugin, ScalaJSPlay)
      // Хз нужен ли этот инклюд сорцов прямо здесь.
      .settings(
        Seq(common).map(p => unmanagedSourceDirectories in Compile <++= unmanagedSourceDirectories in (p, Compile)) : _*
      )
  }

  /** Модуль scala.js для подсистемы внешнего размещения исторически отдельно и он довольно жирный и сложный, чтобы жить внутри дерева lk-sjs. */
  lazy val lkAdvExtSjs = {
    val name = "lk-adv-ext-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlay)
      .dependsOn(commonSjs)
  }

  /** Все мелкие скрипты кроме выдачи (т.е. весь my.suggest.io + буклет и т.д) объеденены в одном большом js. */
  lazy val lkSjs = {
    val name = "lk-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlay)
      .dependsOn(commonSjs, lkAdvExtSjs)
  }

  /** Выдача suggest.io, написанная с помощью scala.js. */
  lazy val scSjs = {
    val name = "sc-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlay)
      .dependsOn(commonSjs)
  }

  /** Утиль, была когда-то расшарена между siobix и sioweb. Постепенно стала просто свалкой. */
  lazy val util = project
    .dependsOn(common, commonPlay)

  /** Внутренний форк securesocial. */
  lazy val securesocial = project
    .enablePlugins(PlayScala, SbtWeb)

  /** веб-интерфейс suggest.io v2. */
  lazy val web21 = project
    .dependsOn(common, util, securesocial)
    .settings(
      scalaJSProjects := Seq(lkSjs, commonSjs, scSjs),
      pipelineStages += scalaJSProd
    )
    .enablePlugins(PlayScala, SbtWeb, PlayScalaJS)
  

  /** Корневой проект. Он должен аггрегировать подпроекты. */
  lazy val root = {
    Project(id = "root", base = file("."))
      .settings(
        scalaVersion := "2.11.6"
      )
      .aggregate(commonPlay, common, lkAdvExtSjs, lkSjs, util, securesocial, scSjs, web21)
  }

  override lazy val settings = super.settings ++ Seq(offline := true)

}
