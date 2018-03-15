package io.suggest.routes

import io.suggest.js.JsRoutesConst.GLOBAL_NAME
import io.suggest.sjs.common.model.Route

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.03.15 11:16
 * Description: Роуты, ВОЗМОЖНО доступные из jsRoutes описываются здесь.
 *
 * Все js-роуты, используемые в sjs-проектах, объявлены здесь, но это не значит, что все эти роуты доступны
 * одновременно или всегда. В шаблоне, чей init-контроллер должен обращаться к какой-то роутере, должен присутствовать
 * вызов к сборке соответствующего javascriptRouter'а.
 */

@js.native
trait IJsRouter extends js.Object {

  /** Все экспортированные контроллеры. */
  def controllers: Controllers = js.native

}

@JSGlobal( GLOBAL_NAME )
@js.native
object routes extends IJsRouter


/** Интерфейс routes.controllers с доступом к static-контроллеру. */
@js.native
sealed trait Controllers extends js.Object {

  /** Роуты для static-контроллера. */
  def Static: StaticCtlRoutes = js.native

  /** Роуты для img-контроллера. */
  def Img: ImgCtlRoutes = js.native

  /** Роуты для assets-контроллера. */
  def Assets: AssetsCtlRoutes = js.native

  /** Роуты для Ident-контроллера. */
  def Ident: IdentCtlRoutes = js.native

  /** Роуты для MarketLkAdn-контроллера. */
  def MarketLkAdn: MarketLkAdnCtlRoutes = js.native

  /** Доступ к HTTP-роутам до серверного контроллера LkAdEdit. */
  def LkAdEdit: LkAdEditCtlRoutes = js.native

}


/** Интерфейс контроллера Static. */
@js.native
sealed trait StaticCtlRoutes extends js.Object {

  def popupCheckContent(): Route = js.native

  /** Роута для доступа к унифицированному websocket channel. */
  def wsChannel(ctxId: String): Route = js.native

}


/** Интерфейс роутера ImgController'а. */
@js.native
sealed trait ImgCtlRoutes extends js.Object {

  /** Форма-окошко для кропа. */
  def imgCropForm(imgId: String, width: Int, height: Int): Route = js.native

}


@js.native
sealed trait AssetsCtlRoutes extends js.Object {

  def versioned(file: String): Route = js.native

  def at(file: String): Route = js.native

}


@js.native
sealed trait IdentCtlRoutes extends js.Object {

  def mySioStartPage(): Route = js.native

  def rdrUserSomewhere(): Route = js.native

}


@js.native
sealed trait MarketLkAdnCtlRoutes extends js.Object {

  def lkList(): Route = js.native

  def showNodeAds(nodeId: String): Route = js.native

}


/** Интерфейс js-роутера для LkAdEdit-контроллера. */
@js.native
sealed trait LkAdEditCtlRoutes extends js.Object {

  def editAd(adId: String): Route = js.native

  def prepareImgUpload(adId: String = null, nodeId: String = null): Route = js.native

  def saveAdSubmit(adId: String = null, producerId: String = null): Route = js.native

  def deleteSubmit(adId: String): Route = js.native

}

