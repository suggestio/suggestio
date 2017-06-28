import play.sbt.PlayScala
import webscalajs._
import sbt._
import Keys._
import com.typesafe.sbt.web._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import org.scalajs.sbtplugin._
import WebScalaJS.autoImport._
import ScalaJSPlugin.autoImport._

import scalajsbundler.sbtplugin.{ScalaJSBundlerPlugin, WebScalaJSBundlerPlugin}
import WebScalaJSBundlerPlugin.autoImport._

object Sio2Build extends Build {

  val DIR0 = "src1/"

  /** Общий код серверной и клиентской частей подсистемы внешнего размещения. */
  lazy val common = (crossProject.crossType( CrossType.Pure ) in file(DIR0 + "shared/common"))
    .settings(
      //resolvers += "sonatype-oss-snapshots" at Common.Repo.SONATYPE_OSS_SNAPSHOTS_URL,    // Нужно только для wix accord *-SNAPSHOT.
      Common.settingsOrg,
      version := "0.0.0-SNAPSHOT",
      testFrameworks += new TestFramework("minitest.runner.Framework"),
      libraryDependencies ++= Seq(
        "io.suzaku"    %%% "boopickle"   % Common.boopickleVsn,
        "com.beachape" %%% "enumeratum"  % Common.enumeratumVsn,
        "io.monix"     %%% "minitest"    % Common.minitestVsn  % Test
        //"com.wix"      %%% "accord-core" % Common.wixAccordVsn
      )
    )
    .jsConfigure(_ enablePlugins ScalaJSWeb)

  lazy val commonJVM = common.jvm.settings(
    name := "commonJVM",
    scalaVersion := Common.SCALA_VSN
  )

  lazy val commonJS = common.js.settings(
    name := "commonJS",
    scalaVersion := Common.SCALA_VSN_JS
  )


  /** Утиль, была когда-то расшарена между siobix и sioweb. Постепенно стала просто свалкой. */
  lazy val util = project
    .in( file(DIR0 + "server/util/util") )
    .dependsOn(commonJVM, logsMacro, srvTestUtil % Test)

  /** Кое-какие общие вещи для js. */
  lazy val commonSjs = {
    val name = "common-sjs"
    Project(id = name, base = file(DIR0 + "client/" ++ name))
      //.enablePlugins(ScalaJSBundlerPlugin)
      .dependsOn(commonJS)
      // Хз нужен ли этот инклюд сорцов прямо здесь.
      /*.settings(
        List(commonJS).map { p =>
          unmanagedSourceDirectories in Compile <++= unmanagedSourceDirectories in (p, Compile)
        } : _*
      )*/
  }

  /** Расшаренная утиль для интеграции с react.js через scalajs-react. */
  lazy val commonReactSjs = {
    Project(id = "scalajs-react-common", base = file(DIR0 + "client/scalajs/react-common"))
      //.enablePlugins(ScalaJSBundlerPlugin)
      .dependsOn(commonSjs)
  }

  /** 2016.jan.22: SVG-утиль свалена выведена в отдельный подпроект из www. */
  lazy val svgUtil = {
    val name = "svg-util"
    Project(name, base = file(DIR0 + "server/media/" + name))
      .dependsOn(logsMacro)
  }

  /** 
   * Модуль scala.js для подсистемы внешнего размещения исторически отдельно и он довольно жирный и сложный,
   * чтобы жить внутри дерева lk-sjs.
   */
  lazy val lkAdvExtSjs = {
    val name = "lk-adv-ext-sjs"
    Project(id = name, base = file(DIR0 + "client/lk/adv/ext"))
      //.enablePlugins(ScalaJSBundlerPlugin)
      .dependsOn(commonSjs)
  }

  /** Трейты для поддержки простых логов. */
  lazy val logsMacro = {
    val name = "logs-macro"
    Project(id = name, base = file(DIR0 + "server/util/" + name))
  }

  /** Поддержка seaweedfs */
  lazy val swfs = project
    .in( file(DIR0 + "server/media/swfs") )
    .dependsOn(util)

  /** Поддержка моделей n2. */
  lazy val n2 = project
    .in( file(DIR0 + "server/nodes/n2") )
    .dependsOn(esUtil, swfs, mgeo, srvTestUtil % Test)

  /**
   * Расширенный pg-драйвер для slick-based моделей в подпроектах.
   * Из-за проблем с classLoader'ами в play и slick, этот подпроект живёт изолировано.
   */
  lazy val commonSlickDriver = {
    val name = "common-slick-driver"
    Project(id = name, base = file(DIR0 + "server/util/" ++ name))
  }

  /** Scala.js API для доступа к jquery.datetimepicker.js от xdsoft. */
  lazy val jqDateTimePickerSjs = {
    val name = "jquery-datetimepicker"
    Project(id = name, base = file(DIR0 + "client/scalajs/jquery/jquery-datetimepicker"))
      //.enablePlugins(ScalaJSBundlerPlugin)
  }

  /** всякая мелочь, специфчная только для личного кабинета, но используется в нескольких модулях. */
  lazy val lkCommonSjs = {
    val name = "lk-common-sjs"
    Project(id = name, base = file(DIR0 + "client/lk/common"))
      //.enablePlugins(ScalaJSBundlerPlugin)
      .dependsOn(commonSjs, commonReactSjs, reactImageGallerySjs)
  }

  /** Самопальные биндинги для moment.js. */
  lazy val momentSjs = {
    Project(id = "moment-sjs", base = file(DIR0 + "client/dt/moment"))
      //.enablePlugins(ScalaJSPlugin)
  }

  /** sio-утиль для moment.js. */
  lazy val momentSioSjs = {
    Project(id = "moment-sio-sjs", base = file(DIR0 + "client/dt/moment-sio"))
      //.enablePlugins(ScalaJSBundlerPlugin)
      .dependsOn(momentSjs, commonSjs)
  }

  /** Фасады scala.js для react-date-picker. */
  lazy val reactDatePickerSjs = {
    Project(id = "scalajs-react-date-picker", base = file(DIR0 + "client/scalajs/react-date-picker"))
      //.enablePlugins(ScalaJSBundlerPlugin)
      .dependsOn(commonReactSjs, momentSioSjs)
  }

  /** Фасады scala.js для react-image-gallery. */
  lazy val reactImageGallerySjs = {
    Project(id = "scalajs-react-image-gallery", base = file(DIR0 + "client/scalajs/react-image-gallery"))
      //.enablePlugins(ScalaJSBundlerPlugin)
      .dependsOn(commonReactSjs)
  }

  /** Утиль поддержки виджета задания периода дат. Расшарена между несколькими lk-модулями. */
  lazy val lkDtPeriodSjs = {
    val name = "lk-dt-period-sjs"
    Project(id = name, base = file(DIR0 + "client/lk/dt-period"))
      //.enablePlugins(ScalaJSBundlerPlugin)
      .dependsOn(lkCommonSjs, jqDateTimePickerSjs, commonReactSjs, reactDatePickerSjs)
  }

  /** lk-adv-common sjs. */
  lazy val lkAdvCommonSjs = {
    val name = "lk-adv-common-sjs"
    Project(id = name, base = file(DIR0 + "client/lk/adv/common"))
      //.enablePlugins(ScalaJSBundlerPlugin)
      .dependsOn(lkCommonSjs, lkDtPeriodSjs, commonReactSjs, mapsSjs)
  }

  /** Поддержка формы прямого размещения на узлах. */
  lazy val lkAdvDirectSjs = {
    val name = "lk-adv-direct-sjs"
    Project(id = name, base = file(DIR0 + "client/lk/adv/direct"))
      //.enablePlugins(ScalaJSBundlerPlugin)
      .dependsOn(lkAdvCommonSjs)
  }

  /** Sjs-модуль для поддержки подсистемы размещения в гео-тегах. */
  lazy val lkAdvGeoSjs = {
    val name = "lk-adv-geo-sjs"
    Project(id = name, base = file(DIR0 + "client/lk/adv/geo"))
      //.enablePlugins(ScalaJSBundlerPlugin)
      .dependsOn(lkAdvCommonSjs, lkTagsEditSjs, leafletMarkerClusterSjs, leafletReactSjs, commonReactSjs, mapsSjs)
  }

  /** Sjs поддержки формы управления узлами/под-узлами в ЛК узла. */
  lazy val lkNodesFormSjs = {
    val name = "lk-nodes-form-sjs"
    Project(id = name, base = file(DIR0 + "client/lk/nodes/form"))
      //.enablePlugins(ScalaJSBundlerPlugin)
      .dependsOn(lkAdvCommonSjs, commonReactSjs)
  }

  /** Поддержка тестов для server-side тестов. Использовать так: .dependsOn(srvTestUtil % Test) */
  lazy val srvTestUtil = {
    Project(id = "srv-test-util", base = file(DIR0 + "server/util/test-util"))
      .dependsOn(logsMacro, commonJVM)
  }

  /** Модели биллинга второго поколения. */
  lazy val mbill2 = project
    .in( file(DIR0 + "server/bill/mbill2") )
    .dependsOn(logsMacro, commonJVM, util, mgeo)

  /** Утиль и модели для поддержки интеграции с БД ipgeobase. */
  lazy val ipgeobase = {
    val name = "ipgeobase"
    Project(id = name, base = file(DIR0 + "server/geo/" + name))
      .dependsOn(logsMacro, esUtil, mgeo)
  }

  /** Подсистема сбора статистики. */
  lazy val stat = project
    .in( file(DIR0 + "server/stat/mstat") )
    .dependsOn(logsMacro, esUtil, mgeo)

  /** Scala.js API для самой cordova. */
  lazy val cordovaSjs = {
    Project(id = "scalajs-cordova", base = file(DIR0 + "client/scalajs/cordova"))
      //.enablePlugins(ScalaJSBundlerPlugin)
  }

  /** scala.js API для evothings/cordova-ble. */
  lazy val cordovaBleSjs = {
    Project(id = "scalajs-cordova-ble", base = file(DIR0 + "client/ble/cordova-ble"))
      //.enablePlugins(ScalaJSBundlerPlugin)
  }

  /** Самописное leaflet API. */
  lazy val leafletSjs = {
    Project(id = "scalajs-leaflet", base = file(DIR0 + "client/geo/leaflet/main"))
      //.enablePlugins(ScalaJSPlugin)
      .dependsOn(commonSjs)
  }

  /** Самописное leaflet-react API. */
  lazy val leafletReactSjs = {
    Project(id = "scalajs-leaflet-react", base = file(DIR0 + "client/geo/leaflet/react"))
      //.enablePlugins(ScalaJSBundlerPlugin)
      .dependsOn(commonSjs, leafletSjs, leafletMarkerClusterSjs, commonReactSjs)
  }

  /** leaflet.markercluster.js scalajs API. */
  lazy val leafletMarkerClusterSjs = {
    Project(id = "scalajs-leaflet-markercluster", base = file(DIR0 + "client/geo/leaflet/markercluster"))
      //.enablePlugins(ScalaJSBundlerPlugin)
      .dependsOn(leafletSjs, commonReactSjs)
  }

  /** Поддержка MaxMind GeoIP2. */
  // Отложено до переезда на elasticsearch 5.x. См. mmgeoip2/README
  lazy val mmgeoip2 = {
    val name = "mmgeoip2"
    Project(id = name, base = file(DIR0 + "server/geo/" + name))
      .dependsOn(util, logsMacro)
  }

  /** mapbox.js API. */
  //lazy val mapBoxSjs = {
  //  val name = "scalajs-mapbox"
  //  Project(id = name, base = file("scalajs/" + name))
  //    .enablePlugins(ScalaJSBundlerPlugin)
  //    .dependsOn(leafletSjs)
  //}

  /** mapbox-gl API. */
  lazy val mapBoxGlSjs = {
    Project(id = "scalajs-mapboxgl", base = file(DIR0 + "client/geo/mapboxgl"))
      //.enablePlugins(ScalaJSPlugin)
      .dependsOn(commonSjs)
  }

  /** Утиль для поддержки географических карт. */
  lazy val mapsSjs = {
    val name = "maps-sjs"
    Project(id = name, base = file(DIR0 + "client/geo/common-maps"))
      //.enablePlugins(ScalaJSBundlerPlugin)
      // TODO lkCommonSjs просто из-за зависимости от diode-react, по факту достаточно commonSjs. Перещёлкнуть, когда и sc-sjs будет реагировать.
      .dependsOn(lkCommonSjs, leafletSjs, commonReactSjs, leafletReactSjs)
  }

  /** Sjs-модуль редактора тегов. */
  lazy val lkTagsEditSjs = {
    val name = "lk-tags-edit-sjs"
    Project(id = name, base = file(DIR0 + "client/lk/tags-edit"))
      //.enablePlugins(ScalaJSBundlerPlugin)
      .dependsOn(lkCommonSjs)
  }

  /** Sjs-поддержка размещения ADN-узла на карте. */
  lazy val lkAdnMapSjs = {
    Project(id = "lk-adn-map-sjs", base = file(DIR0 + "client/lk/adn/lk-adn-map"))
      //.enablePlugins(ScalaJSBundlerPlugin)
      .dependsOn(lkCommonSjs, lkAdvCommonSjs, lkDtPeriodSjs, mapsSjs)
  }

  /** Всякие мелкие скрипты ЛК объеденены в этом scala-js. */
  lazy val lkSjs = {
    Project(id = "lk-sjs", base = file(DIR0 + "client/lk/main"))
      //.enablePlugins(ScalaJSBundlerPlugin, ScalaJSWeb)
      .enablePlugins(ScalaJSWeb)
      .dependsOn(lkAdvExtSjs, lkAdvDirectSjs, lkAdvGeoSjs, lkAdnMapSjs, lkNodesFormSjs)
      // Чтобы clean/test в lk-sjs срабатывал и на зависимых вещах, перечисляем их здесь:
      .aggregate(lkAdvExtSjs, lkAdvDirectSjs, lkAdvGeoSjs, lkAdvCommonSjs, lkCommonSjs, lkAdnMapSjs, lkNodesFormSjs)
  }

  /** scala.js реализация системы мониторинга js-маячков. */
  lazy val bleBeaconerSjs = {
    val name = "ble-beaconer"
    Project(id = name, base = file(DIR0 + "client/ble/" + name))
      //.enablePlugins(ScalaJSBundlerPlugin)
      .dependsOn(commonSjs, cordovaSjs, cordovaBleSjs)
  }

  /** Выдача suggest.io, написанная с помощью scala.js. */
  lazy val scSjs = {
    Project(id = "sc-sjs", base = file(DIR0 + "client/sc/main"))
      //.enablePlugins(ScalaJSBundlerPlugin, ScalaJSWeb)
      .enablePlugins(ScalaJSWeb)
      .dependsOn(commonSjs, mapBoxGlSjs, bleBeaconerSjs, cordovaSjs)
  }

  /** Внутренний форк securesocial. */
  lazy val securesocial = project
    .in( file(DIR0 + "server/id/securesocial") )
    .enablePlugins(PlayScala, SbtWeb)

  /** Модели, связывающие географию, es и sio. */
  lazy val mgeo = project
    .in( file(DIR0 + "server/geo/mgeo") )
    .dependsOn(esUtil, logsMacro, srvTestUtil % Test)

  /** Пошаренная утиль для сборки www-кусков. */
  lazy val commonWww = {
    val id = "common-www"
    Project(id = id, base = file(DIR0 + "server/util/" + id))
      .dependsOn(util, logsMacro, n2, mbill2, secWwwUtil)
  }

  /** security-утиль для веб-морды. */
  lazy val secWwwUtil = {
    val id = "sec-www-util"
    Project(id = id, base = file(DIR0 + "server/util/" + id))
      .dependsOn(util, esUtil, logsMacro, securesocial)
  }

  /** Разная поддержка узлов для вёб-морды. */
  lazy val nodesWww = {
    val id = "nodes-www"
    Project(id = id, base = file(DIR0 + "server/nodes/" + id))
      .dependsOn(commonWww, n2)
  }

  /** Утиль для поддержки ElasticSearch. */
  lazy val esUtil = {
    val id = "es-util"
    Project(id = id, base = file(DIR0 + "server/util/" + id))
      .dependsOn(util, textUtil)
  }

  /** Текстовая утиль, выносимая из util и других мест. */
  lazy val textUtil = {
    val id = "text-util"
    Project(id = id, base = file(DIR0 + "server/util/" + id))
      .dependsOn(util, logsMacro)
  }    // TODO Возможно, зависимость от util не потребуется после окончания рефакторинга. Проверить.

  /** Платежная поддержка для веб-интерфейса. */
  lazy val payWww = {
    val id = "pay-www"
    Project(id = id, base = file(DIR0 + "server/bill/" + id))
      .dependsOn(commonWww, mbill2)
  }


  /** веб-интерфейс suggest.io v2. */
  lazy val www = project
    .in( file(DIR0 + "server/www") )
    .enablePlugins(PlayScala, SbtWeb, WebScalaJSBundlerPlugin)
    .dependsOn(
      securesocial,
      esUtil, mgeo, n2, mbill2,
      nodesWww, payWww,
      textUtil,
      svgUtil, ipgeobase, stat
    )
    .settings(
      scalaJSProjects := Seq(lkSjs, scSjs),
      pipelineStages in Assets += scalaJSPipeline,
      npmAssets ++= NpmAssets.ofProject(reactDatePickerSjs) { nodeModules =>
        (nodeModules / "react-datepicker" / "dist") * "*.min.css"
      }.value,
      npmAssets ++= NpmAssets.ofProject(reactImageGallerySjs) { nodeModules =>
        (nodeModules / "react-image-gallery" / "build") * "*.css"
      }.value,
      npmAssets ++= NpmAssets.ofProject(leafletSjs) { nodeModules =>
        val llcCss = (nodeModules / "leaflet.locatecontrol" / "dist") * "*.min.css"
        val lDist = nodeModules / "leaflet" / "dist"
        val lCss = lDist * "*.css"
        val lImages = (lDist / "images").***
        llcCss +++ lCss +++ lImages
      }.value,
      npmAssets ++= NpmAssets.ofProject(leafletMarkerClusterSjs) { nodeModules =>
        (nodeModules / "leaflet.markercluster" / "dist") * "*.css"
      }.value
    )


  /** Корневой проект. Он должен аггрегировать подпроекты. */
  lazy val sio2 = {
    Project(id = "sio2", base = file("."))
      .settings(Common.settingsOrg: _*)
      .aggregate(
        commonJS, commonJVM, logsMacro,
        commonSjs, commonReactSjs,
        leafletSjs, leafletReactSjs, mapBoxGlSjs,
        lkSjs, scSjs, jqDateTimePickerSjs, momentSjs, reactDatePickerSjs, lkDtPeriodSjs,
        cordovaSjs, cordovaBleSjs, bleBeaconerSjs,
        util, esUtil, textUtil, swfs, n2, securesocial,
        ipgeobase, stat,
        mgeo, commonWww, nodesWww,
        www, mbill2, payWww, secWwwUtil, svgUtil
      )
  }

  // Активация offline-режима резолва зависимостей.
  override lazy val settings = super.settings ++ Seq(offline := true)

}
