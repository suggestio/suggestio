package io.suggest.lk.adn.map

import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.controller.{IInit, InitRouter}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.11.16 14:46
  * Description: Рендер динамических элементов формы размещения узла ADN на карте.
  * Сама форма и её статическое содержимое реднерится на сервере.
  * Так сделано для упрощения первого использования react-велосипеда в проекте.
  */

trait LkAdnMapFormInitRouter extends InitRouter {

  override protected def routeInitTarget(itg: MInitTarget): Future[_] = {
    if (itg == MInitTargets.AdnMapForm) {
      Future {
        (new FormInit)
          .init()
      }
    } else {
      super.routeInitTarget(itg)
    }
  }

}


/** Непосредственный инициализатор формы размещения ADN-узла на карте. */
class FormInit extends IInit {

  /** Запуск инициализации текущего модуля. */
  override def init(): Unit = {
    val mapInitializer = new LkAdnMapInit
    mapInitializer.init()
  }

}
