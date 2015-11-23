package io.suggest.sjs.common.controller._router

import io.suggest.sjs.common.model.Route

import scala.scalajs.js

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

/** Интерфейс routes.controllers с доступом к static-контроллеру. */
@js.native
class Controllers extends js.Object {

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
}


/** Интерфйес роутера ImgController'а. */
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

