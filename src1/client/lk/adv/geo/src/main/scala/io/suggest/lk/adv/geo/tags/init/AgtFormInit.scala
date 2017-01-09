package io.suggest.lk.adv.geo.tags.init

import io.suggest.lk.adv.geo.LkAdvGeoFormCircuit
import io.suggest.lk.adv.geo.r.AdvGeoFormR
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.controller.{IInit, InitRouter}
import io.suggest.sjs.common.vm.spa.PreLoaderLk
import japgolly.scalajs.react.ReactDOM
import org.scalajs.dom

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 10:24
 * Description: Инициализация формы размещения в геотегах.
 */
trait AdvGeoFormInitRouter extends InitRouter {

  override protected def routeInitTarget(itg: MInitTarget): Future[_] = {
    if (itg == MInitTargets.AdvGtagForm) {
      Future {
        new AdvGeoFormInit2()
          .init()
      }
    } else {
      super.routeInitTarget(itg)
    }
  }

}


/** Инициализатор формы георазмещения второго поколения на базе react.js. */
class AdvGeoFormInit2 extends IInit {

  /** Запуск инициализации текущего модуля. */
  override def init(): Unit = {

    // Инициализировать хранилку ссылки на гифку прелоадера, т.к. тот будет стёрт в ходе react-рендера.
    PreLoaderLk.PRELOADER_IMG_URL

    // Для эксперимента сразу активируем url#-роутер внутри одной страницы с одним содержимым.
    // Это наподобии того, что реализовано в scalajs-spa-tutorial.

    val rform = LkAdvGeoFormCircuit.wrap(m => m)(AdvGeoFormR.apply)
    ReactDOM.render(rform, dom.document.getElementById("xynta"))
  }

}
