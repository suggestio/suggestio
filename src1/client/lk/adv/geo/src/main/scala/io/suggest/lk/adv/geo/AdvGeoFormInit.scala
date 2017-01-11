package io.suggest.lk.adv.geo

import io.suggest.adv.geo.AdvGeoConstants
import io.suggest.lk.adv.geo.r.AdvGeoFormR
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.controller.{IInit, InitRouter}
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.spa.PreLoaderLk
import japgolly.scalajs.react.ReactDOM
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLDivElement

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 10:24
 * Description: Инициализация формы размещения в геотегах.
 */
trait AdvGeoFormInitRouter extends InitRouter {

  override protected def routeInitTarget(itg: MInitTarget): Future[_] = {
    if (itg == MInitTargets.AdvGeoForm) {
      Future {
        new AdvGeoFormInit()
          .init()
      }
    } else {
      super.routeInitTarget(itg)
    }
  }

}


/** Инициализатор формы георазмещения второго поколения на базе react.js. */
class AdvGeoFormInit extends IInit {

  /** Запуск инициализации текущего модуля. */
  override def init(): Unit = {

    // Инициализировать хранилку ссылки на гифку прелоадера, т.к. тот будет стёрт входе react-рендера.
    PreLoaderLk.PRELOADER_IMG_URL

    // Для эксперимента сразу активируем url#-роутер внутри одной страницы с одним содержимым.
    // Это наподобии того, что реализовано в scalajs-spa-tutorial.

    val rform = LkAdvGeoFormCircuit.wrap(m => m)(AdvGeoFormR.apply)
    val target = VUtil.getElementByIdOrNull[HTMLDivElement]( AdvGeoConstants.REACT_FORM_TARGET_ID )
    ReactDOM.render( rform, target )
  }

}
