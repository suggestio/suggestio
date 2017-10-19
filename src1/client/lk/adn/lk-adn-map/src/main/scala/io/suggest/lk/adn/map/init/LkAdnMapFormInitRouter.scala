package io.suggest.lk.adn.map.init

import io.suggest.sjs.common.controller.InitRouter
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.11.16 14:46
  * Description: Рендер динамических элементов формы размещения узла ADN на карте.
  * Сама форма и её статическое содержимое реднерится на сервере.
  * Так сделано для упрощения первого использования react-велосипеда в проекте.
  */

trait LkAdnMapFormInitRouter extends InitRouter {

  override protected def routeInitTarget(itg: MInitTarget): Unit = {
    if (itg ==* MInitTargets.AdnMapForm) {
      (new LkAdnMapFormInit)
        .init()
    } else {
      super.routeInitTarget(itg)
    }
  }

}
