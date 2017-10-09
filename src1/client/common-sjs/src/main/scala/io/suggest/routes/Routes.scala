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
  def Static: StaticController = js.native

  /** Роуты для img-контроллера. */
  def Img: ImgController = js.native

  /** Роуты для assets-контроллера. */
  def Assets: AssetsController = js.native

}


/** Интерфейс контроллера Static. */
@js.native
sealed trait StaticController extends js.Object {

  def popupCheckContent(): Route = js.native

  /** Роута для доступа к данным гео.карты рекламщиков.
    * Обычно проходит через CDN, но это уже разруливает серверный js-роутер. */
  def advRcvrsMap(): Route = js.native

}


/** Интерфейс роутера ImgController'а. */
@js.native
sealed trait ImgController extends js.Object {

  /** Форма-окошко для кропа. */
  def imgCropForm(imgId: String, width: Int, height: Int): Route = js.native

}


@js.native
sealed trait AssetsController extends js.Object {

  def versioned(file: String): Route = js.native

  def at(file: String): Route = js.native

}

