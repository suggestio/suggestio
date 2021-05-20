import play.sbt.PlayScala
import sbt._
import Keys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import WebScalaJS.autoImport._

import scalajsbundler.sbtplugin.WebScalaJSBundlerPlugin
import WebScalaJSBundlerPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}


val DIR0 = "src1/"

// Import common settings
Common.settingsOrg

/** Shared client/server code living in "common" subproject. */
lazy val common = crossProject(JSPlatform, JVMPlatform)
  .crossType( CrossType.Full )
  .in( file(DIR0 + "shared/common") )
  .settings(
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    Common.settingsOrg,
    version := "0.0.0",
    testFrameworks += new TestFramework("minitest.runner.Framework"),
    libraryDependencies ++= Seq(
      // Univeral dependencies for client/server.
      "com.typesafe.play" %%% "play-json" % Common.Vsn.PLAY_JSON_VSN,
      "com.beachape" %%% "enumeratum"  % Common.enumeratumVsn,
      "org.scalaz"   %%% "scalaz-core" % Common.Vsn.SCALAZ,
      "com.github.japgolly.univeq"   %%% "univeq-scalaz" % Common.Vsn.UNIVEQ,
      "org.scala-lang.modules" %%% "scala-parser-combinators" % Common.Vsn.SCALA_PARSER_COMBINATORS,
      // diode for FastEq to [common], possibly need to move to js-only with dropping most of FastEq implementations in [common].
      "io.suzaku"    %%% "diode-core"  % Common.diodeVsn,
      // monocle
      "com.github.julien-truffaut" %%%  "monocle-core"  % Common.Vsn.MONOCLE,
      "com.github.julien-truffaut" %%%  "monocle-macro" % Common.Vsn.MONOCLE,
      "com.github.julien-truffaut" %%%  "monocle-law"   % Common.Vsn.MONOCLE % Test,
      // Tests only for [common].
      "io.monix"     %%% "minitest"    % Common.minitestVsn  % Test
    )
  )
  .jsConfigure(_ enablePlugins ScalaJSWeb)

/** Common classes, cross-compiled for server-side. */
lazy val commonJVM = common.jvm
  .settings(
    name := "commonJVM",
    scalaVersion := Common.SCALA_VSN
  )

/** Common classes, cross-compiled for client-side. */
lazy val commonJS = common.js
  .settings(
    name := "commonJS",
    scalaVersion := Common.SCALA_VSN_JS,
    libraryDependencies ++= Seq(
      // java.time client-side support:
      "io.github.cquiroz" %%% "scala-java-time" % Common.Vsn.SCALA_JAVA_TIME,
      // tzdb only for [lk-dt-period-sjs]
    )
  )


/** Historically server-side utils. */
lazy val util = project
  .in( file(DIR0 + "server/util/util") )
  .dependsOn(commonJVM, logsMacro, streamsUtil, srvTestUtil % Test)

/** CommonSjs -- client-side-only JS stuff. */
lazy val commonSjs = {
  val name = "common-sjs"
  Project(id = name, base = file(DIR0 + "client/" ++ name))
    .dependsOn(commonJS)
}


/** Shared client-side react.js and related stuff. */
lazy val commonReactSjs = {
  Project(id = "scalajs-react-common", base = file(DIR0 + "client/scalajs/react-common"))
    .dependsOn(commonSjs)
}

/** Scala.js API for Quill Delta. */
lazy val quillDeltaSjs = {
  Project(id = "scalajs-quill-delta", base = file(DIR0 + "client/scalajs/quill/quill-delta"))
}

/** Scala.js API for Quill.js. */
lazy val quillSjs = {
  Project(id = "scalajs-quill", base = file(DIR0 + "client/scalajs/quill/quill"))
    .dependsOn( quillDeltaSjs )
}

/** Scala.js API for react-quill. */
lazy val reactQuillSjs = {
  Project(id = "scalajs-react-quill", base = file(DIR0 + "client/scalajs/quill/react-quill"))
    .dependsOn(commonReactSjs, quillSjs)
}

/** Suggest.io utils for quill.js, react-quill, delta, etc. */
lazy val quillSioSjs = {
  Project(id = "quill-sio-sjs", base = file(DIR0 + "client/scalajs/quill/quill-sio"))
    .dependsOn(reactQuillSjs, quillDeltaSjs)
}

/** Server-side SVG-utilities. */
lazy val svgUtil = {
  val name = "svg-util"
  Project(name, base = file(DIR0 + "server/media/" + name))
    .dependsOn(logsMacro, commonJVM)
}

/** 
 * Scala.js form for posting suggest.io content to external resources.
 * As of 2021, still not support jd-rendering and without react.js, and disabled.
 */
lazy val lkAdvExtSjs = {
  val name = "lk-adv-ext-sjs"
  Project(id = name, base = file(DIR0 + "client/lk/adv/ext"))
    .dependsOn(commonSjs)
}

/** Ads editor scala.js react form. */
lazy val lkAdEditorSjs = {
  val name = "lk-ad-editor-sjs"
  Project(id = name, base = file(DIR0 + "client/lk/ad/editor"))
    .dependsOn( lkCommonSjs, quillSioSjs, jdEditSjs, asmCryptoSioSjs, reactMaterialUiSjs )
}

/** Traits for server-side macro logging. */
lazy val logsMacro = {
  val name = "logs-macro"
  Project(id = name, base = file(DIR0 + "server/util/" + name))
}

/** SeaWeedFS storage HTTP client. */
lazy val swfs = project
  .in( file(DIR0 + "server/media/swfs") )
  .dependsOn(util, streamsUtil % Test)

/** Nodes-v2 graph models. */
lazy val n2 = project
  .in( file(DIR0 + "server/nodes/n2") )
  .dependsOn(esUtil, swfs, mgeo, srvTestUtil % Test)

/** Slick Postgresql extended driver (profile).
  * Due to old classLoader issues, this driver imported via libdeps as external dependency.
  */
lazy val commonSlickDriver = {
  val name = "common-slick-driver"
  Project(id = name, base = file(DIR0 + "server/util/" ++ name))
}

/** Stuff/utils for users private cabinet, shared between other cabinet modules. */
lazy val lkCommonSjs = {
  val name = "lk-common-sjs"
  Project(id = name, base = file(DIR0 + "client/lk/common"))
    .dependsOn(
      commonSjs, commonReactSjs, reactImageGallerySjs, reactColorSjs,
      reactImageCropSjs, asmCryptoSioSjs, reactMaterialUiSjs, reactDndSjs,
      flowjsSjs, muiTreasurySjs, reactResizableSjs, reactMaterialUiColorSjs,
    )
}

/** Scala.js react login/signup/password-reset forms. */
lazy val loginFormSjs = {
  Project(id = "login-form-sjs", base = file(DIR0 + "client/lk/login/form") )
    .dependsOn( lkCommonSjs )
}

/** Scala.js React.js component/forms for Suggest.io billing Cart. */
lazy val cartSjs = {
  Project(id = "cart-sjs", base = file(DIR0 + "client/bill/cart"))
    .dependsOn( lkCommonSjs, reactMaterialUiSjs, jdRenderSjs )
}

/** Scala.js API form Moment.js. */
lazy val momentSjs = Project(id = "moment-sjs", base = file(DIR0 + "client/dt/moment"))

/** Suggest.io utils for moment.js. */
lazy val momentSioSjs = {
  Project(id = "moment-sio-sjs", base = file(DIR0 + "client/dt/moment-sio"))
    .dependsOn(momentSjs, commonSjs)
}

/** Scala.js API for react-date-picker. */
lazy val reactDatePickerSjs = {
  Project(id = "scalajs-react-date-picker", base = file(DIR0 + "client/scalajs/react-date-picker"))
    .dependsOn(commonReactSjs, momentSioSjs)
}

/** Scala.js API for react-color. */
lazy val reactColorSjs = {
  Project(id = "scalajs-react-color", base = file(DIR0 + "client/scalajs/react-color"))
    .dependsOn(commonReactSjs)
}

/** Scala.js API for jodit. */
lazy val joditSjs = {
  Project(id = "scalajs-jodit", base = file(DIR0 + "client/scalajs/jodit/jodit"))
}

/** Scala.js API for jodit-react. */
lazy val joditReactSjs = {
  Project(id = "scalajs-jodit-react", base = file(DIR0 + "client/scalajs/jodit/jodit-react"))
    .dependsOn( commonReactSjs, joditSjs )
}

/** Scala.js API for react-image-crop. */
lazy val reactImageCropSjs = {
  Project(id = "scalajs-react-image-crop", base = file(DIR0 + "client/scalajs/react-image-crop"))
    .dependsOn(commonReactSjs)
}

/** Scala.js API for react-image-gallery. */
lazy val reactImageGallerySjs = {
  Project(id = "scalajs-react-image-gallery", base = file(DIR0 + "client/scalajs/react-image-gallery"))
    .dependsOn(commonReactSjs)
}

/** Scala.js API for react-grid-layout (TODO unused). */
lazy val reactGridLayoutSjs = {
  val name = "react-grid-layout"
  Project(id = "scalajs-" + name, base = file(s"${DIR0}client/scalajs/$name"))
    .dependsOn( commonReactSjs )
}

/** Moderation react.js form. */
lazy val sysMdrSjs = {
  Project(id = "sys-mdr-sjs", base = file(s"${DIR0}client/sys/mdr"))
    .dependsOn( lkCommonSjs, reactMaterialUiSjs, jdRenderSjs )
}

/** Scala.js API for react-stonecutter. */
lazy val reactStoneCutterSjs = {
  val name = "react-stonecutter"
  Project(id = "scalajs-" + name, base = file(s"${DIR0}client/scalajs/$name"))
    .dependsOn( commonReactSjs )
}

/** Scala.js API for react-measure. */
lazy val reactMeasureSjs = {
  val name = "react-measure"
  Project(id = "scalajs-" + name, base = file(s"${DIR0}client/scalajs/$name"))
    .dependsOn( commonReactSjs )
}

/** Scala.js API for react-resizable. */
lazy val reactResizableSjs = {
  val name = "react-resizable"
  Project(id = "scalajs-" + name, base = file(s"${DIR0}client/scalajs/$name"))
    .dependsOn( commonReactSjs )
}

/** Scala.js API for react-dnd. */
lazy val reactDndSjs = {
  val name = "react-dnd"
  Project(id = "scalajs-" + name, base = file(s"${DIR0}client/scalajs/$name"))
    .dependsOn( commonReactSjs )
}

/** Scala.js API for react-sanfona (TODO unused). */
lazy val reactSanfonaSjs = {
  val name = "react-sanfona"
  Project(id = "scalajs-" + name, base = file(s"${DIR0}client/scalajs/$name"))
    .dependsOn( commonReactSjs )
}

/** Scala.js API for react-sidebar (TODO unused). */
lazy val reactSidebar = {
  val name = "react-sidebar"
  Project(id = "scalajs-" + name, base = file(s"${DIR0}client/scalajs/$name"))
    .dependsOn( commonReactSjs )
}

/** Scala.js API for react-scroll. */
lazy val reactScroll = {
  val name = "react-scroll"
  Project(id = "scalajs-" + name, base = file(s"${DIR0}client/scalajs/$name"))
    .dependsOn( commonReactSjs )
}

/** React.js components for date period forms. */
lazy val lkDtPeriodSjs = {
  val name = "lk-dt-period-sjs"
  Project(id = name, base = file(DIR0 + "client/lk/dt-period"))
    .dependsOn(lkCommonSjs, commonReactSjs, reactDatePickerSjs)
}

/** Common react-components for lk-adv-forms. */
lazy val lkAdvCommonSjs = {
  val name = "lk-adv-common-sjs"
  Project(id = name, base = file(DIR0 + "client/lk/adv/common"))
    .dependsOn(lkCommonSjs, commonReactSjs, mapsSjs)
}

/** Form for geo-advertising of ads in geo-regions, tags, direct adv, etc. */
lazy val lkAdvGeoSjs = {
  val name = "lk-adv-geo-sjs"
  Project(id = name, base = file(DIR0 + "client/lk/adv/geo"))
    .dependsOn(lkAdvCommonSjs, lkTagsEditSjs, leafletMarkerClusterSjs, leafletReactSjs, commonReactSjs, mapsSjs, lkDtPeriodSjs)
}

/** React.js form for Nodes management. */
lazy val lkNodesFormSjs = {
  val name = "lk-nodes-form-sjs"
  Project(id = name, base = file(DIR0 + "client/lk/nodes/form"))
    .dependsOn(lkAdvCommonSjs, commonReactSjs)
}

/** Some server-side test utils.
  * To use: .dependsOn(srvTestUtil % Test) */
lazy val srvTestUtil = {
  Project(id = "srv-test-util", base = file(DIR0 + "server/util/test-util"))
    .dependsOn(logsMacro, commonJVM)
}

/** Slick SQL billing models for 2nd-generation billing. */
lazy val mbill2 = project
  .in( file(DIR0 + "server/bill/mbill2") )
  .dependsOn(logsMacro, commonJVM, util, mgeo, commonSlickDriver)

/** IPGeoBase server-side utils for fetching/parsing/indexing ipgeobase.ru database dumps. */
lazy val ipgeobase = {
  val name = "ipgeobase"
  Project(id = name, base = file(DIR0 + "server/geo/" + name))
    .dependsOn(logsMacro, esUtil, mgeo)
}

/** Server-side statistics collection stuff. */
lazy val stat = project
  .in( file(DIR0 + "server/stat/mstat") )
  .dependsOn(logsMacro, esUtil, mgeo)

/** Scala.js API for Apache Cordova and some cordova plugin's APIs. */
lazy val cordovaSjs = {
  Project(id = "scalajs-cordova", base = file(DIR0 + "client/scalajs/cordova"))
    .dependsOn( commonSjs )
}

/** Scala.js API for cordova-ble-central. */
lazy val cordovaBleSjs = Project(id = "scalajs-cordova-ble", base = file(DIR0 + "client/ble/cordova-ble"))

/** Suggest.io util/addons over Apache Cordova APIs. */
lazy val cordovaSioUtilSjs = {
  val name = "cordova-sio-util"
  Project(id = name, base = file(DIR0 + "client/util/" + name))
    .dependsOn( commonSjs, cordovaSjs )
}

/** Scala.js API for WebBluetooth. */
//lazy val webBluetoothSjs = {
//  val name = "web-bluetooth"
//  Project(id = "scalajs-" + name, base = file(DIR0 + "client/ble/" + name))
//}

/** Scala.js API for Leaflet.js */
lazy val leafletSjs = Project(id = "scalajs-leaflet", base = file(DIR0 + "client/geo/leaflet/main"))
    .dependsOn(commonSjs)

/** Scala.js API for react-leaflet. */
lazy val leafletReactSjs = {
  Project(id = "scalajs-leaflet-react", base = file(DIR0 + "client/geo/leaflet/react"))
    .dependsOn(commonSjs, leafletSjs, leafletMarkerClusterSjs, commonReactSjs)
}

/** Scala.js API for Leaflet.MarkerCluster.js */
lazy val leafletMarkerClusterSjs = {
  Project(id = "scalajs-leaflet-markercluster", base = file(DIR0 + "client/geo/leaflet/markercluster"))
    .dependsOn(leafletSjs, commonReactSjs)
}

/** MaxMind GeoIP2 stuff (TODO unused, not yet implemented, see mmgeoip2/README). */
lazy val mmgeoip2 = {
  val name = "mmgeoip2"
  Project(id = name, base = file(DIR0 + "server/geo/" + name))
    .dependsOn(util, logsMacro)
}

/** Scala.js API facades for asmcrypto.js */
lazy val asmCryptoJsSjs = {
  Project(id = "scalajs-asmcryptojs", base = file(DIR0 + "client/scalajs/asmcrypto/asmcryptojs"))
}

/** Suggest.io extensions over asmCrypto.js APIs. */
lazy val asmCryptoSioSjs = {
  Project(id = "asmcrypto-sio-sjs", base = file(DIR0 + "client/scalajs/asmcrypto/asmcrypto-sio"))
    .dependsOn( asmCryptoJsSjs, commonSjs )
    .aggregate( asmCryptoJsSjs )
}

/** Scala.js API for Mapbox-gl.js (TODO unused). */
lazy val mapBoxGlSjs = {
  Project(id = "scalajs-mapboxgl", base = file(DIR0 + "client/geo/mapboxgl"))
    .dependsOn(commonSjs)
}

/** Suggest.io client-side utils and MVCs for geo.maps and related things. */
lazy val mapsSjs = {
  val name = "maps-sjs"
  Project(id = name, base = file(DIR0 + "client/geo/common-maps"))
    .dependsOn(commonSjs, leafletSjs, commonReactSjs, leafletReactSjs)
}

/** Tags editor react-components. */
lazy val lkTagsEditSjs = {
  val name = "lk-tags-edit-sjs"
  Project(id = name, base = file(DIR0 + "client/lk/tags-edit"))
    .dependsOn(lkCommonSjs)
}

/** React-form for nodes placements on the map as logo. */
lazy val lkAdnMapSjs = {
  val prefix = "lk-adn-map"
  Project(id = prefix + "-sjs", base = file(DIR0 + "client/lk/adn/" + prefix))
    .dependsOn(lkCommonSjs, lkAdvCommonSjs, lkDtPeriodSjs, mapsSjs, lkDtPeriodSjs)
}

/** React-form for editing node metadata (name, address, etc). */
lazy val lkAdnEditSjs = {
  Project(id = "lk-adn-edit-sjs", base = file(DIR0 + "client/lk/adn/edit"))
    .dependsOn(lkCommonSjs)
}

/** React form for ads list inside personal cabinet. */
lazy val lkAdsSjs = {
  Project(id = "lk-ads-sjs", base = file(DIR0 + "client/lk/ads"))
    // lkNodesSjs: integration by API now, full nodes sub-form popup integration in future.
    .dependsOn(lkCommonSjs, jdRenderSjs, lkNodesFormSjs, reactStoneCutterSjs, reactScroll)
}

/** Merged top-level js of personal cabinet.
  * Aggregates all related cabinet react-forms.
  */
lazy val lkSjs = {
  Project(id = "lk-sjs", base = file(DIR0 + "client/lk/main"))
    .enablePlugins(WebScalaJS)
    .dependsOn(
      lkAdvExtSjs, lkAdvGeoSjs, lkAdnMapSjs, lkNodesFormSjs, lkAdEditorSjs,
      lkAdsSjs, lkAdnEditSjs, cartSjs, sysMdrSjs, loginFormSjs,
    )
}

/** Merged react-forms in one JS for /sys/ area. */
lazy val sysSjs = {
  Project(id = "sys-sjs", base = file(DIR0 + "client/sys/main"))
    .enablePlugins( WebScalaJS )
    .dependsOn(
      sysMdrSjs,
      sysEdgeEditSjs,
    )
}

/** BluetoothLE Listener for detection of radio-beacons (Scala.js). */
lazy val bleBeaconerSjs = {
  val name = "ble-beaconer"
  Project(id = name, base = file(DIR0 + "client/ble/" + name))
    .dependsOn(commonSjs, cordovaSjs, cordovaBleSjs)
}

/** Scala.js API for ServiceWorker toolbox. */
lazy val swToolBoxSjs = {
  val id = "sw-toolbox"
  Project(id = id + "-sjs", base = file(DIR0 + "client/scalajs/" + id))
}

/** Some showcase stuff outside sc3-sjs. */
lazy val scCommonSjs = {
  Project(id = "sc-common", base = file(DIR0 + "client/sc/common"))
    .dependsOn( commonSjs, reactMaterialUiSjs )
}

/** Showcase 3rd-generation -- react.js main single-page-application of Suggest.io. */
lazy val sc3Sjs = {
  Project(id = "sc3-sjs", base = file(DIR0 + "client/sc/v3"))
    .enablePlugins(WebScalaJS)
    .dependsOn(
      scCommonSjs, commonReactSjs, bleBeaconerSjs, cordovaSioUtilSjs,
      mapsSjs, jdRenderSjs, reactScroll, reactQrCodeSjs,
      loginFormSjs, lkNodesFormSjs,
    )
}

/** ServiceWorker for showcase (sc3Sjs). */
lazy val scSwSjs = {
  Project(id = "sc-sw-sjs", base = file(DIR0 + "client/sc/sw"))
    .enablePlugins( WebScalaJS )
    .dependsOn( commonSjs, swToolBoxSjs )
}

/** Scala.js API for React MaterialUI. */
lazy val reactMaterialUiSjs = {
  Project(id = "scalajs-react-materialui", base = file(DIR0 + "client/scalajs/react-materialui"))
    .dependsOn( commonReactSjs )
}

/** Scala.js API for mui-treasury (material-ui addons). */
lazy val muiTreasurySjs = {
  Project(id = "scalajs-mui-treasury", base = file(DIR0 + "client/scalajs/mui-treasury"))
    .dependsOn( reactMaterialUiSjs )
}

/** Scala.js API for materialui-color color-picker. */
lazy val reactMaterialUiColorSjs = {
  Project( id = "scalajs-react-materialui-color", base = file(DIR0 + "client/scalajs/react-materialui-color") )
    .dependsOn( reactMaterialUiSjs )
}

/** React-form for editing node edges with file-uploading support. */
lazy val sysEdgeEditSjs = {
  Project(id = "sys-edge-edit-sjs", base = file(DIR0 + "client/sys/edge-edit"))
    .dependsOn( lkCommonSjs, scCommonSjs, reactMaterialUiSjs )
}

/** JD (Json Document) tree rendering components. */
lazy val jdRenderSjs = {
  Project(id = "jd-render-sjs", base = file(DIR0 + "client/jd/jd-render"))
    .dependsOn( commonSjs, reactStoneCutterSjs, reactMeasureSjs, reactStoneCutterSjs, reactScroll )
}

/** JD editing components. */
lazy val jdEditSjs = {
  Project(id = "jd-edit-sjs", base = file(DIR0 + "client/jd/jd-edit"))
    .dependsOn( lkCommonSjs, jdRenderSjs )
}

/** Securesocial internal parts (TODO legacy, remove/merge?). */
lazy val securesocial = project
  .in( file(DIR0 + "server/id/securesocial") )
  .enablePlugins(PlayScala, SbtWeb)
  .dependsOn( secWwwUtil )

/** Server-side geographical models and utils. */
lazy val mgeo = project
  .in( file(DIR0 + "server/geo/mgeo") )
  .dependsOn(esUtil, logsMacro, srvTestUtil % Test)

/** Server-side utilities extracted from [www]. */
lazy val commonWww = {
  val id = "common-www"
  Project(id = id, base = file(DIR0 + "server/util/" + id))
    .dependsOn(util, logsMacro, n2, mbill2, secWwwUtil)
}

/** Some security-related classes for server-side www. */
lazy val secWwwUtil = {
  val id = "sec-www-util"
  Project(id = id, base = file(DIR0 + "server/util/" + id))
    .dependsOn(util, esUtil, logsMacro, n2)
}

/** Anti-virus software integration classes. */
lazy val secAvUtil = {
  val id = "sec-av-util"
  Project(id = id, base = file(DIR0 + "server/util/" + id))
    .dependsOn(util, streamsUtil, logsMacro)
}

/** Some nodes-related stuff for server-side. */
lazy val nodesWww = {
  val id = "nodes-www"
  Project(id = id, base = file(DIR0 + "server/nodes/" + id))
    .dependsOn(commonWww, n2)
}

/** ElasticSearch utilities. */
lazy val esUtil = {
  val id = "es-util"
  Project(id = id, base = file(DIR0 + "server/util/" + id))
    .dependsOn(util, textUtil)
}

/** Some additional classes over akka-streams. */
lazy val streamsUtil = {
  val id = "streams-util"
  Project(id = id, base = file(DIR0 + "server/util/" + id))
    .dependsOn(logsMacro, commonJVM)
}

/** Utilities for brotli compression on server. */
lazy val brotliUtil = {
  val id = "brotli-util"
  Project(id = id, base = file(DIR0 + "server/util/" + id))
    .dependsOn(streamsUtil)
}

/** Text processing server-side classes. */
lazy val textUtil = {
  val id = "text-util"
  Project(id = id, base = file(DIR0 + "server/util/" + id))
    .dependsOn(util, logsMacro)
}

/** Scala.js API for react-qrcode. */
lazy val reactQrCodeSjs = {
  val id = "scalajs-react-qrcode"
  Project(id = id, base = file(DIR0 + "client/scalajs/react-qrcode"))
    .dependsOn( commonReactSjs )
}

/** Scala.js API for Flow.js.
  * @see [[https://github.com/flowjs/flow.js/]]
  */
lazy val flowjsSjs = {
  val name = "scalajs-flowjs"
  Project(id = name, base = file(DIR0 + "client/scalajs/flowjs"))
}


/** Suggest.io server WEB/HTTP interface.*/
lazy val www = project
  .in( file(DIR0 + "server/www") )
  .enablePlugins(PlayScala, WebScalaJSBundlerPlugin)
  .dependsOn(
    securesocial, secAvUtil,
    esUtil, mgeo, n2, mbill2,
    nodesWww,
    streamsUtil, brotliUtil,
    textUtil,
    svgUtil, ipgeobase, stat
  )
  .settings(
    scalaJSProjects := Seq(lkSjs, sc3Sjs, scSwSjs, sysSjs),
    pipelineStages in Assets := Seq(scalaJSPipeline),
    // Copy some assets from npm:
    // react DatePicker
    npmAssets ++= NpmAssets.ofProject(reactDatePickerSjs) { nodeModules =>
      (nodeModules / "react-datepicker" / "dist") * "*.min.css"
    }.value,
    // react-image-gallery
    npmAssets ++= NpmAssets.ofProject(reactImageGallerySjs) { nodeModules =>
      (nodeModules / "react-image-gallery" / "styles" / "css") * "*.css"
    }.value,
    // leaflet LocateControl
    npmAssets ++= NpmAssets.ofProject(leafletSjs) { nodeModules =>
      val llcCss = (nodeModules / "leaflet.locatecontrol" / "dist") * "*.min.css"
      val lDist = nodeModules / "leaflet" / "dist"
      val lCss = lDist * "*.css"
      val lImages = (lDist / "images").allPaths
      llcCss +++ lCss +++ lImages
    }.value,
    // leaflet MarkerCluster
    npmAssets ++= NpmAssets.ofProject(leafletMarkerClusterSjs) { nodeModules =>
      (nodeModules / "@glartek" / "leaflet.markercluster" / "dist") * "*.css"
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
    }.value,
    // react-resizeable
    npmAssets ++= NpmAssets.ofProject( reactResizableSjs ) { nodeModules =>
      (nodeModules / "react-resizable" / "css") * "*.css"
    }.value,
  )


/** Aggregate project for client-side sub-projects (compiled to js). */
lazy val client = project
  .in( file(DIR0 + "client") )
  .settings(Common.settingsOrg: _*)
  .aggregate(
    commonJS,
    commonSjs, commonReactSjs,
    reactMaterialUiSjs, reactQrCodeSjs, muiTreasurySjs, reactMaterialUiColorSjs,
    leafletSjs, leafletReactSjs, leafletMarkerClusterSjs, leafletReactSjs, lkAdvGeoSjs,
    lkAdvCommonSjs, mapsSjs,
    lkSjs, sysSjs,
    scCommonSjs, sc3Sjs,
    scSwSjs, swToolBoxSjs,
    momentSjs, reactDatePickerSjs, momentSioSjs, lkDtPeriodSjs,
    cordovaSjs, cordovaBleSjs, cordovaSioUtilSjs, bleBeaconerSjs,
    reactImageGallerySjs, reactColorSjs, reactImageCropSjs,
    reactGridLayoutSjs, reactStoneCutterSjs,
    reactScroll, reactMeasureSjs, reactDndSjs,
    quillDeltaSjs, quillSjs, reactQuillSjs, quillSioSjs, joditReactSjs, joditSjs,
    lkAdEditorSjs, lkAdnEditSjs,
    lkAdsSjs, lkTagsEditSjs, lkAdnMapSjs, lkAdvExtSjs, lkNodesFormSjs, lkCommonSjs,
    asmCryptoJsSjs, asmCryptoSioSjs,
    sysMdrSjs, loginFormSjs, sysEdgeEditSjs,
    cartSjs,
    flowjsSjs,
  )


/** Aggregate for server-side sub-projects (compiled for JVM). */
lazy val server = project
  .in( file(DIR0 + "server") )
  .settings(Common.settingsOrg: _*)
  .aggregate(
    commonJVM, logsMacro,
    srvTestUtil,
    streamsUtil, brotliUtil,
    util, esUtil, textUtil, swfs, n2, securesocial,
    ipgeobase, stat,
    mgeo, commonWww, nodesWww,
    commonSlickDriver, mbill2,
    secWwwUtil, secAvUtil, svgUtil,
    www
  )


/** Root project aggregate. */
lazy val sio2 = project
  .in( file(".") )
  .settings(Common.settingsOrg: _*)
  .aggregate(
    client,
    server
  )


/** Diffenent sub-projects, not used anymore. */
/*
lazy val trash = project
  .in( file(DIR0) )
  .settings(Common.settingsOrg: _*)
  .aggregate(
    mmgeoip2,
    reactSanfonaSjs, reactResizableSjs, mapBoxGlSjs,
  )
*/
