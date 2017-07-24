package io.suggest.routes

import io.suggest.sjs.common.model.Route

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.07.17 18:41
  * Description: Статические роуты, пошаренные по всему проекту, т.к. используются и в lk, и в sc3.
  */

@js.native
trait StaticRoutesController extends js.Object {

  def Static: StaticRoutes = js.native

}


@js.native
trait StaticRoutes extends js.Object {

  /** Роута для доступа к данным гео.карты рекламщиков.
    * Обычно проходит через CDN, но это уже разруливает серверный js-роутер. */
  def advRcvrsMap(): Route = js.native

}

