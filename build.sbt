import play.sbt.PlayScala
import sbt._
import Keys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import WebScalaJS.autoImport._

import scalajsbundler.sbtplugin.WebScalaJSBundlerPlugin
import WebScalaJSBundlerPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}


val DIR0 = "src1/"

// Активация offline-режима резолва зависимостей, и выставление прочих самых базовых сеттингов:
Common.settingsOrg

/** Общий код серверной и клиентской частей подсистемы внешнего размещения. */
lazy val common = crossProject(JSPlatform, JVMPlatform)
  .crossType( CrossType.Full )
  .in( file(DIR0 + "shared/common") )
  .settings(
    // TODO Надо избавляться от SNAPSHOT в коде.
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    Common.settingsOrg,
    version := "0.0.0",
    testFrameworks += new TestFramework("minitest.runner.Framework"),
    libraryDependencies ++= Seq(
      // Универсальные зависимости для клиента и сервера. Наследуются во ВСЕ компоненты проекта.
      // Сериализация:
      // Быстрая-компактная-ломучая бинарщина boopickle, которую надо удалить в пользу play-json.
      "io.suzaku"    %%% "boopickle"   % Common.boopickleVsn,
      "com.typesafe.play" %%% "play-json" % Common.Vsn.PLAY_JSON_VSN,
      // Вместо scala.Enumeration используем сие:
      "com.beachape" %%% "enumeratum"  % Common.enumeratumVsn,
      // scalaz появилась для Validation.
      "org.scalaz"   %%% "scalaz-core" % Common.Vsn.SCALAZ,
      // UnivEq позволяет избегать fruitless comparison даже с дженериками.
      "com.github.japgolly.univeq"   %%% "univeq-scalaz" % Common.Vsn.UNIVEQ,
      // ScalaCSS генерит CSS в выдаче.
      "com.github.japgolly.scalacss" %%% "core"          % Common.Vsn.SCALACSS,
      // Комбинируемые парсеры. Раньше они не были доступны для scala.js, поэтому жили только на сервере.
      "org.scala-lang.modules" %%% "scala-parser-combinators" % Common.Vsn.SCALA_PARSER_COMBINATORS,
      // diode для FastEq в [common], а не только в js.
      "io.suzaku"    %%% "diode-core"  % Common.diodeVsn,
      // monocle
      "com.github.julien-truffaut" %%%  "monocle-core"  % Common.Vsn.MONOCLE,
      "com.github.julien-truffaut" %%%  "monocle-macro" % Common.Vsn.MONOCLE,
      "com.github.julien-truffaut" %%%  "monocle-law"   % Common.Vsn.MONOCLE % Test,
      // Тесты **не*наследуется**, только на [common].
      "io.monix"     %%% "minitest"    % Common.minitestVsn  % Test
    )
  )
  .jsConfigure(_ enablePlugins ScalaJSWeb)

lazy val commonJVM = common.jvm
  .settings(
    name := "commonJVM",
    scalaVersion := Common.SCALA_VSN
  )

/** common для client-only-сборки. */
lazy val commonJS = common.js
  .settings(
    name := "commonJS",
    scalaVersion := Common.SCALA_VSN_JS,
    libraryDependencies ++= Seq(
      // java.time-поддержка на клиенте:
      "io.github.cquiroz" %%% "scala-java-time" % "2.+"
    )
  )


/** Утиль, была когда-то расшарена между siobix и sioweb. Постепенно стала просто свалкой. */
lazy val util = project
  .in( file(DIR0 + "server/util/util") )
  .dependsOn(commonJVM, logsMacro, streamsUtil, srvTestUtil % Test)

/** Кое-какие общие вещи для js. */
lazy val commonSjs = {
  val name = "common-sjs"
  Project(id = name, base = file(DIR0 + "client/" ++ name))
    .dependsOn(commonJS)
}

/** Для доступа к react test-utils, используется такая шпилька в конфигурации:
  * Testing - https://github.com/japgolly/scalajs-react/blob/master/doc/TESTING.md#setup
  */
lazy val libDepsReactTest = {
  libraryDependencies ++= Seq(
    "com.github.japgolly.scalajs-react" %%% "test" % Common.reactSjsVsn % Test
  )
}

/** Расшаренная утиль для интеграции с react.js через scalajs-react. */
lazy val commonReactSjs = {
  Project(id = "scalajs-react-common", base = file(DIR0 + "client/scalajs/react-common"))
    .dependsOn(commonSjs)
    .settings( libDepsReactTest )   // Чисто чтобы понимать, что эта конфигурация работает
}

/** Sjs-фасад для JSON-формата Quill Delta. */
lazy val quillDeltaSjs = {
  Project(id = "scalajs-quill-delta", base = file(DIR0 + "client/scalajs/quill/quill-delta"))
}

/** Sjs-фасад для quill.js */
lazy val quillSjs = {
  Project(id = "scalajs-quill", base = file(DIR0 + "client/scalajs/quill/quill"))
    .dependsOn( quillDeltaSjs )
}

/** Sjs-фасад для react-quill. */
lazy val reactQuillSjs = {
  Project(id = "scalajs-react-quill", base = file(DIR0 + "client/scalajs/quill/react-quill"))
    .dependsOn(commonReactSjs, quillSjs)
}

/** sio-утиль для sjs-фасадов quill, react-quill, quill-delta и прочих смежных вещей. */
lazy val quillSioSjs = {
  Project(id = "quill-sio-sjs", base = file(DIR0 + "client/scalajs/quill/quill-sio"))
    .dependsOn(reactQuillSjs, quillDeltaSjs)
}

/** 2016.jan.22: SVG-утиль свалена выведена в отдельный подпроект из www. */
lazy val svgUtil = {
  val name = "svg-util"
  Project(name, base = file(DIR0 + "server/media/" + name))
    .dependsOn(logsMacro, commonJVM)
}

/** 
 * Модуль scala.js для подсистемы внешнего размещения исторически отдельно и он довольно жирный и сложный,
 * чтобы жить внутри дерева lk-sjs.
 */
lazy val lkAdvExtSjs = {
  val name = "lk-adv-ext-sjs"
  Project(id = name, base = file(DIR0 + "client/lk/adv/ext"))
    .dependsOn(commonSjs)
}

/** Редактор рекламных карточкек на базе scala.js. */
lazy val lkAdEditorSjs = {
  val name = "lk-ad-editor-sjs"
  Project(id = name, base = file(DIR0 + "client/lk/ad/editor"))
    .dependsOn( lkCommonSjs, quillSioSjs, jdEditSjs, asmCryptoSioSjs, reactMaterialUiSjs )
}

/** Трейты для поддержки простых логов. */
lazy val logsMacro = {
  val name = "logs-macro"
  Project(id = name, base = file(DIR0 + "server/util/" + name))
}

/** Поддержка seaweedfs */
lazy val swfs = project
  .in( file(DIR0 + "server/media/swfs") )
  .dependsOn(util, streamsUtil % Test)

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

/** всякая мелочь, специфчная только для личного кабинета, но используется в нескольких модулях. */
lazy val lkCommonSjs = {
  val name = "lk-common-sjs"
  Project(id = name, base = file(DIR0 + "client/lk/common"))
    .dependsOn(commonSjs, commonReactSjs, reactImageGallerySjs, reactColorSjs,
               reactImageCropSjs, asmCryptoSioSjs, reactMaterialUiSjs, reactDndSjs)
}

/** Форма логина sjs. */
lazy val loginFormSjs = {
  Project(id = "login-form-sjs", base = file(DIR0 + "client/lk/login/form") )
    .dependsOn( commonSjs, lkCommonSjs, commonReactSjs, reactMaterialUiSjs )
}

/** Компоненты для покупательской корзины suggest.io. */
lazy val cartSjs = {
  Project(id = "cart-sjs", base = file(DIR0 + "client/bill/cart"))
    .dependsOn( lkCommonSjs, reactMaterialUiSjs, jdRenderSjs )
}

/** Самопальные биндинги для moment.js. */
lazy val momentSjs = Project(id = "moment-sjs", base = file(DIR0 + "client/dt/moment"))

/** sio-утиль для moment.js. */
lazy val momentSioSjs = {
  Project(id = "moment-sio-sjs", base = file(DIR0 + "client/dt/moment-sio"))
    .dependsOn(momentSjs, commonSjs)
}

/** Фасады scala.js для react-date-picker. */
lazy val reactDatePickerSjs = {
  Project(id = "scalajs-react-date-picker", base = file(DIR0 + "client/scalajs/react-date-picker"))
    .dependsOn(commonReactSjs, momentSioSjs)
}

/** Scala.js API для react-color. */
lazy val reactColorSjs = {
  Project(id = "scalajs-react-color", base = file(DIR0 + "client/scalajs/react-color"))
    .dependsOn(commonReactSjs)
}

/** Scala.js API для react-image-crop. */
lazy val reactImageCropSjs = {
  Project(id = "scalajs-react-image-crop", base = file(DIR0 + "client/scalajs/react-image-crop"))
    .dependsOn(commonReactSjs)
}

/** Фасады scala.js для react-image-gallery. */
lazy val reactImageGallerySjs = {
  Project(id = "scalajs-react-image-gallery", base = file(DIR0 + "client/scalajs/react-image-gallery"))
    .dependsOn(commonReactSjs)
}

/** Sjs-фасады для react-grid-layout. */
// TODO Унести на гитхаб и удалить окончательно из проекта
lazy val reactGridLayoutSjs = {
  val name = "react-grid-layout"
  Project(id = "scalajs-" + name, base = file(s"${DIR0}client/scalajs/$name"))
    .dependsOn( commonReactSjs )
}

/** Форма модерации. */
lazy val sysMdrSjs = {
  Project(id = "sys-mdr-sjs", base = file(s"${DIR0}client/sys/mdr"))
    .dependsOn( lkCommonSjs, reactMaterialUiSjs, jdRenderSjs )
}

/** Scala.js-фасад для компонентов react-stonecutter (реализация grid layout). */
lazy val reactStoneCutterSjs = {
  val name = "react-stonecutter"
  Project(id = "scalajs-" + name, base = file(s"${DIR0}client/scalajs/$name"))
    .dependsOn( commonReactSjs )
}

/** Scala.js-фасад для react-measure. */
lazy val reactMeasureSjs = {
  val name = "react-measure"
  Project(id = "scalajs-" + name, base = file(s"${DIR0}client/scalajs/$name"))
    .dependsOn( commonReactSjs )
}

/** Scala.js facade for react-resizable. */
lazy val reactResizableSjs = {
  val name = "react-resizable"
  Project(id = "scalajs-" + name, base = file(s"${DIR0}client/scalajs/$name"))
    .dependsOn( commonReactSjs )
}

/** React drag-n-drop фасады scala-js. */
lazy val reactDndSjs = {
  val name = "react-dnd"
  Project(id = "scalajs-" + name, base = file(s"${DIR0}client/scalajs/$name"))
    .dependsOn( commonReactSjs )
    .settings( libDepsReactTest )
}

/** Scala.js-фасад для компонентов в react-sanfona. */
// TODO Унести на гитхаб и удалить окончательно из проекта
lazy val reactSanfonaSjs = {
  val name = "react-sanfona"
  Project(id = "scalajs-" + name, base = file(s"${DIR0}client/scalajs/$name"))
    .dependsOn( commonReactSjs )
}

/** Scala.js биндинги для react-sidebar компонентов. */
lazy val reactSidebar = {
  val name = "react-sidebar"
  Project(id = "scalajs-" + name, base = file(s"${DIR0}client/scalajs/$name"))
    .dependsOn( commonReactSjs )
}

/** Scala.js биндинги для компонентов react-scroll. */
lazy val reactScroll = {
  val name = "react-scroll"
  Project(id = "scalajs-" + name, base = file(s"${DIR0}client/scalajs/$name"))
    .dependsOn( commonReactSjs )
}

/** Утиль поддержки виджета задания периода дат. Расшарена между несколькими lk-модулями. */
lazy val lkDtPeriodSjs = {
  val name = "lk-dt-period-sjs"
  Project(id = name, base = file(DIR0 + "client/lk/dt-period"))
    .dependsOn(lkCommonSjs, commonReactSjs, reactDatePickerSjs)
}

/** lk-adv-common sjs. */
lazy val lkAdvCommonSjs = {
  val name = "lk-adv-common-sjs"
  Project(id = name, base = file(DIR0 + "client/lk/adv/common"))
    .dependsOn(lkCommonSjs, lkDtPeriodSjs, commonReactSjs, mapsSjs)
}

/** Sjs-модуль для поддержки подсистемы размещения в гео-тегах. */
lazy val lkAdvGeoSjs = {
  val name = "lk-adv-geo-sjs"
  Project(id = name, base = file(DIR0 + "client/lk/adv/geo"))
    .dependsOn(lkAdvCommonSjs, lkTagsEditSjs, leafletMarkerClusterSjs, leafletReactSjs, commonReactSjs, mapsSjs)
}

/** Sjs поддержки формы управления узлами/под-узлами в ЛК узла. */
lazy val lkNodesFormSjs = {
  val name = "lk-nodes-form-sjs"
  Project(id = name, base = file(DIR0 + "client/lk/nodes/form"))
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
  .dependsOn(logsMacro, commonJVM, util, mgeo, commonSlickDriver)

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
lazy val cordovaSjs = Project(id = "scalajs-cordova", base = file(DIR0 + "client/scalajs/cordova"))

/** scala.js API для evothings/cordova-ble. */
lazy val cordovaBleSjs = Project(id = "scalajs-cordova-ble", base = file(DIR0 + "client/ble/cordova-ble"))

/** S.io sjs-утиль поверх нативной кордовы. */
lazy val cordovaSioUtilSjs = {
  val name = "cordova-sio-util"
  Project(id = name, base = file(DIR0 + "client/util/" + name))
    .dependsOn( commonSjs, cordovaSjs )
}

/** Scala.js фасады для доступа к web-bluetooth API. */
//lazy val webBluetoothSjs = {
//  val name = "web-bluetooth"
//  Project(id = "scalajs-" + name, base = file(DIR0 + "client/ble/" + name))
//}

/** Самописное leaflet API. */
lazy val leafletSjs = Project(id = "scalajs-leaflet", base = file(DIR0 + "client/geo/leaflet/main"))
    .dependsOn(commonSjs)

/** Самописное leaflet-react API. */
lazy val leafletReactSjs = {
  Project(id = "scalajs-leaflet-react", base = file(DIR0 + "client/geo/leaflet/react"))
    .dependsOn(commonSjs, leafletSjs, leafletMarkerClusterSjs, commonReactSjs)
}

/** leaflet.markercluster.js scalajs API. */
lazy val leafletMarkerClusterSjs = {
  Project(id = "scalajs-leaflet-markercluster", base = file(DIR0 + "client/geo/leaflet/markercluster"))
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
//    .dependsOn(leafletSjs)
//}

/** Scala.js API facades for asmcrypto.js library. */
lazy val asmCryptoJsSjs = {
  Project(id = "scalajs-asmcryptojs", base = file(DIR0 + "client/scalajs/asmcrypto/asmcryptojs"))
}

/** Sio-утиль для asmCrypto.js. */
lazy val asmCryptoSioSjs = {
  Project(id = "asmcrypto-sio-sjs", base = file(DIR0 + "client/scalajs/asmcrypto/asmcrypto-sio"))
    .dependsOn( asmCryptoJsSjs, commonSjs )
    .aggregate( asmCryptoJsSjs )
}

/** mapbox-gl API. */
lazy val mapBoxGlSjs = {
  Project(id = "scalajs-mapboxgl", base = file(DIR0 + "client/geo/mapboxgl"))
    .dependsOn(commonSjs)
}

/** Утиль для поддержки географических карт. */
lazy val mapsSjs = {
  val name = "maps-sjs"
  Project(id = name, base = file(DIR0 + "client/geo/common-maps"))
    .dependsOn(commonSjs, leafletSjs, commonReactSjs, leafletReactSjs)
}

/** Sjs-модуль редактора тегов. */
lazy val lkTagsEditSjs = {
  val name = "lk-tags-edit-sjs"
  Project(id = name, base = file(DIR0 + "client/lk/tags-edit"))
    .dependsOn(lkCommonSjs)
}

/** Sjs-поддержка размещения ADN-узла на карте. */
lazy val lkAdnMapSjs = {
  val prefix = "lk-adn-map"
  Project(id = prefix + "-sjs", base = file(DIR0 + "client/lk/adn/" + prefix))
    .dependsOn(lkCommonSjs, lkAdvCommonSjs, lkDtPeriodSjs, mapsSjs)
}

/** Scala.js формы редактирования метаданных узла. */
lazy val lkAdnEditSjs = {
  Project(id = "lk-adn-edit-sjs", base = file(DIR0 + "client/lk/adn/edit"))
    .dependsOn(lkCommonSjs)
}

/** JS страницы управления карточками узла в личном кабинете. */
lazy val lkAdsSjs = {
  Project(id = "lk-ads-sjs", base = file(DIR0 + "client/lk/ads"))
    // Связь с lkNodes: интеграция сначала по api, а затем со всей nodes-формой.
    .dependsOn(lkCommonSjs, jdRenderSjs, lkNodesFormSjs, reactStoneCutterSjs, reactScroll)
}

/** Всякие мелкие скрипты ЛК объеденены в этом scala-js.
  * Чтобы clean/test в lk-sjs срабатывал и на зависимых вещах, перечисляем их здесь.
  */
lazy val lkSjs = {
  Project(id = "lk-sjs", base = file(DIR0 + "client/lk/main"))
    .enablePlugins(WebScalaJS)
    .dependsOn(
      lkAdvExtSjs, lkAdvGeoSjs, lkAdnMapSjs, lkNodesFormSjs, lkAdEditorSjs,
      lkAdsSjs, lkAdnEditSjs, cartSjs, sysMdrSjs, loginFormSjs
    )
}

/** scala.js реализация системы мониторинга js-маячков. */
lazy val bleBeaconerSjs = {
  val name = "ble-beaconer"
  Project(id = name, base = file(DIR0 + "client/ble/" + name))
    .dependsOn(commonSjs, cordovaSjs, cordovaBleSjs)
}

/** ServiceWorker toolbox. */
lazy val swToolBoxSjs = {
  val id = "sw-toolbox"
  Project(id = id + "-sjs", base = file(DIR0 + "client/scalajs/" + id))
}

/** Утили для выдачи. Общий код выдач разных поколений. */
lazy val scCommonSjs = {
  Project(id = "sc-common", base = file(DIR0 + "client/sc/common"))
    .dependsOn( commonSjs )
}

/** Выдача на scala.js+react. */
lazy val sc3Sjs = {
  Project(id = "sc3-sjs", base = file(DIR0 + "client/sc/v3"))
    .enablePlugins(WebScalaJS)
    // Поддержка BLE и Cordova не реализована. Надо извлечь из прошлой выдачи, которая была удалена 2018-04-03 после 7390c4e0af497795438e67b57e42c28281a100d2 из src1/client/sc/main
    .dependsOn(
      scCommonSjs, commonReactSjs, bleBeaconerSjs, cordovaSioUtilSjs,
      mapsSjs, jdRenderSjs, reactSidebar, reactScroll,
      reactMaterialUiSjs
    )
}

/** ServiceWorker для выдачи. */
lazy val scSwSjs = {
  Project(id = "sc-sw-sjs", base = file(DIR0 + "client/sc/sw"))
    .enablePlugins( WebScalaJS )
    .dependsOn( commonSjs, swToolBoxSjs )
}

/** Экспорт material-ui core+icons с поправками для новой версии. */
lazy val reactMaterialUiSjs = {
  Project(id = "scalajs-react-materialui", base = file(DIR0 + "client/scalajs/react-materialui"))
    .dependsOn( commonReactSjs )
}

/** json document react renderer */
lazy val jdRenderSjs = {
  Project(id = "jd-render-sjs", base = file(DIR0 + "client/jd/jd-render"))
    .dependsOn( commonSjs, reactStoneCutterSjs, reactMeasureSjs, reactStoneCutterSjs, reactScroll )
}

/** Поддержка редактирования jd с кучей доп.зависимостей ЛК. */
lazy val jdEditSjs = {
  Project(id = "jd-edit-sjs", base = file(DIR0 + "client/jd/jd-edit"))
    .dependsOn( lkCommonSjs, jdRenderSjs )
}

/** Внутренний форк securesocial. */
lazy val securesocial = project
  .in( file(DIR0 + "server/id/securesocial") )
  .enablePlugins(PlayScala, SbtWeb)
  .dependsOn( secWwwUtil )

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
    .dependsOn(util, esUtil, logsMacro)
}

/** Утиль для взаимодействия с антивирусами. */
lazy val secAvUtil = {
  val id = "sec-av-util"
  Project(id = id, base = file(DIR0 + "server/util/" + id))
    .dependsOn(util, streamsUtil, logsMacro)
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

lazy val streamsUtil = {
  val id = "streams-util"
  Project(id = id, base = file(DIR0 + "server/util/" + id))
    .dependsOn(logsMacro, commonJVM)
}

/** Sio-утиль для brotli. */
lazy val brotliUtil = {
  val id = "brotli-util"
  Project(id = id, base = file(DIR0 + "server/util/" + id))
    .dependsOn(streamsUtil)
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
  .enablePlugins(PlayScala, WebScalaJSBundlerPlugin)
  .dependsOn(
    securesocial, secAvUtil,
    esUtil, mgeo, n2, mbill2,
    nodesWww, payWww,
    streamsUtil, brotliUtil,
    textUtil,
    svgUtil, ipgeobase, stat
  )
  .settings(
    scalaJSProjects := Seq(lkSjs, sc3Sjs, scSwSjs),
    pipelineStages in Assets ++= Seq(scalaJSPipeline),
    // Скопипастить некоторые ассеты прямо из npm:
    // react DatePicker
    npmAssets ++= NpmAssets.ofProject(reactDatePickerSjs) { nodeModules =>
      (nodeModules / "react-datepicker" / "dist") * "*.min.css"
    }.value,
    // react-image-gallery
    npmAssets ++= NpmAssets.ofProject(reactImageGallerySjs) { nodeModules =>
      (nodeModules / "react-image-gallery" / "build") * "*.css"
    }.value,
    // leaflet LocateControl
    npmAssets ++= NpmAssets.ofProject(leafletSjs) { nodeModules =>
      val llcCss = (nodeModules / "leaflet.locatecontrol" / "dist") * "*.min.css"
      val lDist = nodeModules / "leaflet" / "dist"
      val lCss = lDist * "*.css"
      val lImages = (lDist / "images").allPaths
      // Из-за использования этого через webpack externals: // TODO Надо ли это, если переезд на prunecluster?
      val lJs = lDist * "leaflet.js"
      llcCss +++ lCss +++ lImages +++ lJs
    }.value,
    // leaflet MarkerCluster
    npmAssets ++= NpmAssets.ofProject(leafletMarkerClusterSjs) { nodeModules =>
      (nodeModules / "leaflet.markercluster" / "dist") * "*.css"
    }.value,
    // quill
    npmAssets ++= NpmAssets.ofProject(reactQuillSjs) { nodeModules =>
      val rootPath = nodeModules / "quill"
      val assets = (rootPath / "assets").allPaths
      val distCss = (rootPath / "dist") * "*.css"
      assets +++ distCss
    }.value,
    // react-image-crop
    npmAssets ++= NpmAssets.ofProject( reactImageCropSjs ) { nodeModules =>
      (nodeModules / "react-image-crop" / "dist") * "*.css"
    }.value
  )


/** Корневой проект. Он должен аггрегировать подпроекты. */
lazy val sio2 = {
  Project(id = "sio2", base = file("."))
    .settings(Common.settingsOrg: _*)
    .aggregate(
      commonJS, commonJVM, logsMacro,
      commonSjs, commonReactSjs,
      reactMaterialUiSjs,
      leafletSjs, leafletReactSjs, leafletMarkerClusterSjs, leafletReactSjs, lkAdvGeoSjs,
      lkSjs,
      scCommonSjs, sc3Sjs,
      scSwSjs, swToolBoxSjs,
      momentSjs, reactDatePickerSjs, lkDtPeriodSjs,
      cordovaSjs, cordovaBleSjs, cordovaSioUtilSjs, bleBeaconerSjs,
      reactImageGallerySjs, reactColorSjs, reactImageCropSjs,
      reactGridLayoutSjs, reactStoneCutterSjs,
      reactSidebar, reactScroll, reactMeasureSjs, reactDndSjs,
      quillDeltaSjs, quillSjs, reactQuillSjs, quillSioSjs,
      lkAdEditorSjs, lkAdnEditSjs,
      lkAdsSjs, lkTagsEditSjs, lkAdnMapSjs, lkAdvExtSjs, lkNodesFormSjs, lkCommonSjs,
      streamsUtil, brotliUtil,
      asmCryptoJsSjs, asmCryptoSioSjs,
      sysMdrSjs, loginFormSjs,
      util, esUtil, textUtil, swfs, n2, securesocial,
      ipgeobase, stat,
      mgeo, commonWww, nodesWww,
      mbill2, payWww, cartSjs,
      secWwwUtil, secAvUtil, svgUtil,
      www
    )
}

