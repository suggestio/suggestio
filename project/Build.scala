import play.sbt.PlayScala
import webscalajs._
import sbt._
import Keys._
import com.typesafe.sbt.web._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import org.scalajs.sbtplugin._
import WebScalaJS.autoImport._

object Sio2Build extends Build {


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
      .enablePlugins(ScalaJSPlugin)
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
      .enablePlugins(ScalaJSPlugin)
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
   */
  lazy val commonSlickDriver = {
    val name = "common-slick-driver"
    Project(id = name, base = file(name))
  }

  /** Scala.js API для доступа к jquery.datetimepicker.js от xdsoft. */
  lazy val dateTimePickerSjs = {
    val name = "datetimepicker-scalajs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlugin)
  }

  /** всякая мелочь, специфчная только для личного кабинета, но используется в нескольких модулях. */
  lazy val lkCommonSjs = {
    val name = "lk-common-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(commonSjs)
  }

  /** Утиль поддержки виджета задания периода дат. Расшарена между несколькими lk-модулями. */
  lazy val lkDtPeriodSjs = {
    val name = "lk-dt-period-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(lkCommonSjs, dateTimePickerSjs)
  }

  /** lk-adv-common sjs. */
  lazy val lkAdvCommonSjs = {
    val name = "lk-adv-common-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(lkCommonSjs, lkDtPeriodSjs)
  }

  /** Поддержка формы прямого размещения на узлах. */
  lazy val lkAdvDirectSjs = {
    val name = "lk-adv-direct-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(lkAdvCommonSjs)
  }

  /** Sjs-модуль для поддержки подсистемы размещения в гео-тегах. */
  lazy val lkAdvGeoSjs = {
    val name = "lk-adv-geo-tags-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(lkAdvCommonSjs, lkTagsEditSjs, mapRadSjs, leafletMarketClusterSjs, leafletReactSjs)
  }

  /** Модели биллинга второго поколения. */
  lazy val mbill2 = project
    .dependsOn(logsMacro, common, util)

  /** Утиль и модели для поддержки интеграции с БД ipgeobase. */
  lazy val ipgeobase = {
    val name = "ipgeobase"
    Project(id = name, base = file("loc/geo/" + name))
      .dependsOn(logsMacro, util)
  }

  /** Подсистема сбора статистики. */
  lazy val stat = project
    .dependsOn(logsMacro, util)

  /** Scala.js API для самой cordova. */
  lazy val cordovaSjs = {
    val name = "scalajs-cordova"
    Project(id = name, base = file("scalajs/" + name))
      .enablePlugins(ScalaJSPlugin)
  }

  /** scala.js API для evothings/cordova-ble. */
  lazy val cordovaBleSjs = {
    val name = "scalajs-cordova-ble"
    Project(id = name, base = file("scalajs/" + name))
      .enablePlugins(ScalaJSPlugin)
  }

  /** scala.js API + js для evothings/libs/util.js */
  lazy val evothingsUtilSjs = {
    val name = "scalajs-evothings-util"
    Project(id = name, base = file("scalajs/" + name))
      .enablePlugins(ScalaJSPlugin)
  }

  /** Самописное leaflet API. */
  lazy val leafletSjs = {
    val name = "scalajs-leaflet"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(commonSjs)
  }

  /** Самописное leaflet-react API. */
  lazy val leafletReactSjs = {
    val name = "scalajs-leaflet-react"
    Project(id = name, base = file("scalajs/scalajs-leaflet-react"))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(commonSjs, leafletSjs)
  }

  /** leaflet.markercluster.js scalajs API. */
  lazy val leafletMarketClusterSjs = {
    val name = "scalajs-leaflet-markercluster"
    Project(id = name, base = file("scalajs/" + name))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(leafletSjs)
  }

  /** Поддержка MaxMind GeoIP2. */
  // Отложено до переезда на elasticsearch 5.x. См. mmgeoip2/README
  lazy val mmgeoip2 = {
    val name = "mmgeoip2"
    Project(id = name, base = file("loc/geo/" + name))
      .dependsOn(util, logsMacro)
  }

  /** mapbox.js API. */
  lazy val mapBoxSjs = {
    val name = "scalajs-mapbox"
    Project(id = name, base = file("scalajs/" + name))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(leafletSjs)
  }

  /** mapbox-gl API. */
  lazy val mapBoxGlSjs = {
    val name = "scalajs-mapboxgl"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(commonSjs)
  }

  /** Утиль для поддержки географических карт. */
  lazy val mapsSjs = {
    val name = "maps-sjs"
    Project(id = name, base = file("maps/" + name))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(commonSjs, leafletSjs)
  }

  /** Модуль поддержки карты с возможностью задания радиуса покрытия. */
  lazy val mapRadSjs = {
    val name = "map-rad-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(commonSjs, mapsSjs, leafletSjs)
  }
  
  /** Sjs-модуль редактора тегов. */
  lazy val lkTagsEditSjs = {
    val name = "lk-tags-edit-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(lkCommonSjs)
  }

  /** Sjs-поддержка размещения ADN-узла на карте. */
  lazy val lkAdnMapSjs = {
    val name = "lk-adn-map-sjs"
    Project(id = name, base = file("lk/adn/" + name))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(lkCommonSjs, lkAdvCommonSjs, lkDtPeriodSjs, mapsSjs)
  }

  /** Всякие мелкие скрипты ЛК объеденены в этом scala-js. */
  lazy val lkSjs = {
    val name = "lk-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
      .dependsOn(lkAdvExtSjs, lkAdvDirectSjs, lkAdvGeoSjs, lkAdnMapSjs)
      // Чтобы clean/test в lk-sjs срабатывал и на зависимых вещах, перечисляем их здесь:
      .aggregate(lkAdvExtSjs, lkAdvDirectSjs, lkAdvGeoSjs, lkAdvCommonSjs, lkCommonSjs, lkAdnMapSjs)
  }

  /** scala.js реализация системы мониторинга js-маячков. */
  lazy val bleBeaconerSjs = {
    val name = "ble-beaconer-sjs"
    Project(id = name, base = file("ble/" + name))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(commonSjs, cordovaSjs, cordovaBleSjs, evothingsUtilSjs)
  }

  /** Выдача suggest.io, написанная с помощью scala.js. */
  lazy val scSjs = {
    val name = "sc-sjs"
    Project(id = name, base = file(name))
      .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
      .dependsOn(commonSjs, mapBoxGlSjs, bleBeaconerSjs, cordovaSjs)
  }

  /** Внутренний форк securesocial. */
  lazy val securesocial = project
    .enablePlugins(PlayScala, SbtWeb)

  /** веб-интерфейс suggest.io v2. */
  lazy val web21 = project
    .dependsOn(common, util, securesocial, n2, mbill2, svgUtil, ipgeobase, stat)
    .settings(
      scalaJSProjects := Seq(lkSjs, scSjs),
      pipelineStages in Assets += scalaJSPipeline
    )
    .enablePlugins(PlayScala, SbtWeb)
  

  /** Корневой проект. Он должен аггрегировать подпроекты. */
  lazy val root = {
    Project(id = "root", base = file("."))
      .settings(Common.settingsOrg: _*)
      .aggregate(
        common, logsMacro,
        commonSjs, leafletSjs, leafletReactSjs, mapBoxGlSjs, mapRadSjs, lkSjs, scSjs, dateTimePickerSjs, lkDtPeriodSjs,
        evothingsUtilSjs, cordovaSjs, cordovaBleSjs, bleBeaconerSjs,
        util, swfs, n2, securesocial,
        ipgeobase, stat,
        web21, mbill2, svgUtil
      )
  }

  // Активация offline-режима резолва зависимостей.
  override lazy val settings = super.settings ++ Seq(offline := true)

}
