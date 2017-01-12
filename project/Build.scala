import play.sbt.PlayScala
import webscalajs._
import sbt._
import Keys._
import com.typesafe.sbt.web._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import org.scalajs.sbtplugin._
import WebScalaJS.autoImport._
import ScalaJSPlugin.autoImport._

object Sio2Build extends Build {

  val DIR0 = "src1/"

  /** Общий код серверной и клиентской частей подсистемы внешнего размещения. */
  lazy val common = (crossProject.crossType( CrossType.Pure ) in file(DIR0 + "shared/common"))
    .settings(
      resolvers += "sonatype-oss-snapshots" at Common.Repo.SONATYPE_OSS_SNAPSHOTS_URL,    // Нужно только для wix accord *-SNAPSHOT.
      libraryDependencies ++= Seq(
        "me.chrons"    %%% "boopickle"   % Common.boopickleVsn,
        "com.beachape" %%% "enumeratum"  % Common.enumeratumVsn
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
    .dependsOn(commonJVM, logsMacro)

  /** Кое-какие общие вещи для js. */
  lazy val commonSjs = {
    val name = "common-sjs"
    Project(id = name, base = file(DIR0 + "client/" ++ name))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(commonJS, evothingsUtilSjs)
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
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(commonSjs)
  }

  /** 2016.jan.22: SVG-утиль свалена выведена в отдельный подпроект из web21. */
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
      .enablePlugins(ScalaJSPlugin)
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
    .dependsOn(util, swfs)

  /** 
   * Расширенный pg-драйвер для slick-based моделей в подпроектах.
   * Из-за проблем с classLoader'ами в play и slick, этот подпроект живёт изолировано.
   */
  lazy val commonSlickDriver = {
    val name = "common-slick-driver"
    Project(id = name, base = file(DIR0 + "server/util/" ++ name))
  }

  /** Scala.js API для доступа к jquery.datetimepicker.js от xdsoft. */
  lazy val dateTimePickerSjs = {
    val name = "jquery-datetimepicker"
    Project(id = name, base = file(DIR0 + "client/scalajs/jquery/jquery-datetimepicker"))
      .enablePlugins(ScalaJSPlugin)
  }

  /** всякая мелочь, специфчная только для личного кабинета, но используется в нескольких модулях. */
  lazy val lkCommonSjs = {
    val name = "lk-common-sjs"
    Project(id = name, base = file(DIR0 + "client/lk/common"))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(commonSjs, commonReactSjs)
  }

  /** Утиль поддержки виджета задания периода дат. Расшарена между несколькими lk-модулями. */
  lazy val lkDtPeriodSjs = {
    val name = "lk-dt-period-sjs"
    Project(id = name, base = file(DIR0 + "client/lk/dt-period"))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(lkCommonSjs, dateTimePickerSjs, commonReactSjs)
  }

  /** lk-adv-common sjs. */
  lazy val lkAdvCommonSjs = {
    val name = "lk-adv-common-sjs"
    Project(id = name, base = file(DIR0 + "client/lk/adv/common"))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(lkCommonSjs, lkDtPeriodSjs, commonReactSjs)
  }

  /** Поддержка формы прямого размещения на узлах. */
  lazy val lkAdvDirectSjs = {
    val name = "lk-adv-direct-sjs"
    Project(id = name, base = file(DIR0 + "client/lk/adv/direct"))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(lkAdvCommonSjs)
  }

  /** Sjs-модуль для поддержки подсистемы размещения в гео-тегах. */
  lazy val lkAdvGeoSjs = {
    val name = "lk-adv-geo-sjs"
    Project(id = name, base = file(DIR0 + "client/lk/adv/geo"))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(lkAdvCommonSjs, lkTagsEditSjs, leafletMarketClusterSjs, leafletReactSjs, commonReactSjs, mapsSjs)
  }

  /** Модели биллинга второго поколения. */
  lazy val mbill2 = project
    .in( file(DIR0 + "server/bill/mbill2") )
    .dependsOn(logsMacro, commonJVM, util)

  /** Утиль и модели для поддержки интеграции с БД ipgeobase. */
  lazy val ipgeobase = {
    val name = "ipgeobase"
    Project(id = name, base = file(DIR0 + "server/geo/" + name))
      .dependsOn(logsMacro, util)
  }

  /** Подсистема сбора статистики. */
  lazy val stat = project
    .in( file(DIR0 + "server/stat/mstat") )
    .dependsOn(logsMacro, util)

  /** Scala.js API для самой cordova. */
  lazy val cordovaSjs = {
    val name = "scalajs-cordova"
    Project(id = "scalajs-cordova", base = file(DIR0 + "client/scalajs/cordova"))
      .enablePlugins(ScalaJSPlugin)
  }

  /** scala.js API для evothings/cordova-ble. */
  lazy val cordovaBleSjs = {
    Project(id = "scalajs-cordova-ble", base = file(DIR0 + "client/ble/cordova-ble"))
      .enablePlugins(ScalaJSPlugin)
  }

  /** scala.js API + js для evothings/libs/util.js */
  lazy val evothingsUtilSjs = {
    Project(id = "scalajs-evothings-util", base = file(DIR0 + "client/ble/evothings-util"))
      .enablePlugins(ScalaJSPlugin)
  }
  
  /** Самописное leaflet API. */
  lazy val leafletSjs = {
    Project(id = "scalajs-leaflet", base = file(DIR0 + "client/geo/leaflet/main"))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(commonSjs)
  }

  /** Самописное leaflet-react API. */
  lazy val leafletReactSjs = {
    Project(id = "scalajs-leaflet-react", base = file(DIR0 + "client/geo/leaflet/react"))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(commonSjs, leafletSjs, leafletMarketClusterSjs, commonReactSjs)
  }

  /** leaflet.markercluster.js scalajs API. */
  lazy val leafletMarketClusterSjs = {
    Project(id = "scalajs-leaflet-markercluster", base = file(DIR0 + "client/geo/leaflet/markercluster"))
      .enablePlugins(ScalaJSPlugin)
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
  //    .enablePlugins(ScalaJSPlugin)
  //    .dependsOn(leafletSjs)
  //}

  /** mapbox-gl API. */
  lazy val mapBoxGlSjs = {
    Project(id = "scalajs-mapboxgl", base = file(DIR0 + "client/geo/mapboxgl"))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(commonSjs)
  }

  /** Утиль для поддержки географических карт. */
  lazy val mapsSjs = {
    val name = "maps-sjs"
    Project(id = name, base = file(DIR0 + "client/geo/common-maps"))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(commonSjs, leafletSjs)
  }
  
  /** Sjs-модуль редактора тегов. */
  lazy val lkTagsEditSjs = {
    val name = "lk-tags-edit-sjs"
    Project(id = name, base = file(DIR0 + "client/lk/tags-edit"))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(lkCommonSjs)
  }

  /** Sjs-поддержка размещения ADN-узла на карте. */
  lazy val lkAdnMapSjs = {
    Project(id = "lk-adn-map-sjs", base = file(DIR0 + "client/lk/adn/lk-adn-map"))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(lkCommonSjs, lkAdvCommonSjs, lkDtPeriodSjs, mapsSjs)
  }

  /** Всякие мелкие скрипты ЛК объеденены в этом scala-js. */
  lazy val lkSjs = {
    Project(id = "lk-sjs", base = file(DIR0 + "client/lk/main"))
      .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
      .dependsOn(lkAdvExtSjs, lkAdvDirectSjs, lkAdvGeoSjs, lkAdnMapSjs)
      // Чтобы clean/test в lk-sjs срабатывал и на зависимых вещах, перечисляем их здесь:
      .aggregate(lkAdvExtSjs, lkAdvDirectSjs, lkAdvGeoSjs, lkAdvCommonSjs, lkCommonSjs, lkAdnMapSjs)
  }

  /** scala.js реализация системы мониторинга js-маячков. */
  lazy val bleBeaconerSjs = {
    val name = "ble-beaconer"
    Project(id = name, base = file(DIR0 + "client/ble/" + name))
      .enablePlugins(ScalaJSPlugin)
      .dependsOn(commonSjs, cordovaSjs, cordovaBleSjs, evothingsUtilSjs)
  }

  /** Выдача suggest.io, написанная с помощью scala.js. */
  lazy val scSjs = {
    Project(id = "sc-sjs", base = file(DIR0 + "client/sc/main"))
      .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
      .dependsOn(commonSjs, mapBoxGlSjs, bleBeaconerSjs, cordovaSjs)
  }

  /** Внутренний форк securesocial. */
  lazy val securesocial = project
    .in( file(DIR0 + "server/id/securesocial") )
    .enablePlugins(PlayScala, SbtWeb)

  /** веб-интерфейс suggest.io v2. */
  lazy val web21 = project
    .in( file(DIR0 + "server/www") )
    .dependsOn(commonJVM, util, securesocial, n2, mbill2, svgUtil, ipgeobase, stat)
    .settings(
      scalaJSProjects := Seq(lkSjs, scSjs),
      pipelineStages in Assets += scalaJSPipeline
    )
    .enablePlugins(PlayScala, SbtWeb)
  

  /** Корневой проект. Он должен аггрегировать подпроекты. */
  lazy val sio2 = {
    Project(id = "sio2", base = file("."))
      .settings(Common.settingsOrg: _*)
      .aggregate(
        commonJS, commonJVM, logsMacro,
        commonSjs, commonReactSjs,
        leafletSjs, leafletReactSjs, mapBoxGlSjs,
        lkSjs, scSjs, dateTimePickerSjs, lkDtPeriodSjs,
        evothingsUtilSjs, cordovaSjs, cordovaBleSjs, bleBeaconerSjs,
        util, swfs, n2, securesocial,
        ipgeobase, stat,
        web21, mbill2, svgUtil
      )
  }

  // Активация offline-режима резолва зависимостей.
  override lazy val settings = super.settings ++ Seq(offline := true)

}
