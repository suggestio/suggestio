import play.sbt.PlayScala
import playscalajs.{PlayScalaJS, ScalaJSPlay}
import sbt._
import Keys._
import com.typesafe.sbt.web._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import org.scalajs.sbtplugin._
import PlayScalaJS.autoImport._

object SiobixBuild extends Build {


  /** Общий код серверной и клиентской частей подсистемы внешнего размещения. */
  lazy val common = {
    val name = "common"
    Project(id = name, base = file(name))
  }

  /** Утиль, была когда-то расшарена между siobix и sioweb. Постепенно стала просто свалкой. */
  lazy val util = project
    .dependsOn(common, logsMacro)

  /** Кое-какие общие вещи для js. */
  lazy val commonSjs = {
    val name = "common-sjs"
    Project(id = name, base = file(name))
      .dependsOn(common)
      .enablePlugins(ScalaJSPlugin, ScalaJSPlay)
      // Хз нужен ли этот инклюд сорцов прямо здесь.
      .settings(
        List(common).map { p =>
          unmanagedSourceDirectories in Compile <++= unmanagedSourceDirectories in (p, Compile)
        } : _*
      )
  }

  /** 2016.jan.22: SVG-утиль свалена выведена в отдельный подпроект из web21. */
  lazy val svgUtil = {
    val name = "svg-util"
    Project(name, base = file(name))
      .dependsOn(logsMacro)
  }

  /** 
   * Модуль scala.js для подсистемы внешнего размещения исторически отдельно и он довольно жирный и сложный,
   * чтобы жить внутри дерева lk-sjs.
   */
  lazy val lkAdvExtSjs = {
    val name = "lk-adv-ext-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlay)
      .dependsOn(commonSjs)
  }

  /** Трейты для поддержки простых логов. */
  lazy val logsMacro = {
    val name = "logs-macro"
    Project(id = name, base = file(name))
  }

  /** Поддержка seaweedfs */
  lazy val swfs = project
    .dependsOn(util)

  /** Поддержка моделей n2. */
  lazy val n2 = project
    .dependsOn(util, swfs)

  /** 
   * Расширенный pg-драйвер для slick-based моделей в подпроектах.
   * Из-за проблем с classLoader'ами в play и slick, этот подпроект живёт изолировано.
   * @see [[]]
   */
  lazy val commonSlickDriver = {
    val name = "common-slick-driver"
    Project(id = name, base = file(name))
  }

  /** Модели биллинга второго поколения. */
  lazy val mbill2 = project
    .dependsOn(logsMacro, common, util)

  /** Самописное leaflet API. */
  lazy val leafletSjs = {
    val name = "scalajs-leaflet"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlay)
      .dependsOn(commonSjs)
  }

  /** Модуль поддержки карты. */
  lazy val mapRadSjs = {
    val name = "map-rad-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlay)
      .dependsOn(commonSjs, leafletSjs)
  }

  /** Все мелкие скрипты кроме выдачи (т.е. весь my.suggest.io + буклет и т.д) объеденены в одном большом js. */
  lazy val lkSjs = {
    val name = "lk-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlay)
      .dependsOn(commonSjs, lkAdvExtSjs, mapRadSjs)
  }

  /** Выдача suggest.io, написанная с помощью scala.js. */
  lazy val scSjs = {
    val name = "sc-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlay)
      .dependsOn(commonSjs)
  }

  /** Внутренний форк securesocial. */
  lazy val securesocial = project
    .enablePlugins(PlayScala, SbtWeb)

  /** веб-интерфейс suggest.io v2. */
  lazy val web21 = project
    .dependsOn(common, util, securesocial, n2, mbill2, svgUtil)
    .settings(
      scalaJSProjects := Seq(lkSjs, commonSjs, scSjs, mapRadSjs, leafletSjs),
      pipelineStages += scalaJSProd
    )
    .enablePlugins(PlayScala, SbtWeb, PlayScalaJS)
  

  /** Корневой проект. Он должен аггрегировать подпроекты. */
  lazy val root = {
    Project(id = "root", base = file("."))
      .settings(Common.settingsOrg: _*)
      .aggregate(common, lkAdvExtSjs, leafletSjs, mapRadSjs, lkSjs, util, swfs, n2, securesocial, scSjs, web21, svgUtil)
  }

  // Активация offline-режима резолва зависимостей.
  override lazy val settings = super.settings ++ Seq(offline := true)

}
