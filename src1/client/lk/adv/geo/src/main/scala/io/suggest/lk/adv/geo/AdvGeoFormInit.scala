package io.suggest.lk.adv.geo

import io.suggest.adv.AdvConstants
import io.suggest.adv.geo.AdvGeoConstants
import io.suggest.init.routed.InitRouter
import io.suggest.lk.adv.m.MPriceS.MPriceSFastEq
import io.suggest.lk.adv.r.PriceR
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.spa.LkPreLoader
import org.scalajs.dom.raw.HTMLDivElement
import japgolly.scalajs.react.vdom.Implicits._
import japgolly.univeq._
import com.softwaremill.macwire._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 10:24
 * Description: Инициализация формы размещения в геотегах.
 */
trait AdvGeoFormInitRouter extends InitRouter {

  override protected def routeInitTarget(itg: MJsInitTarget): Unit = {
    if (itg ==* MJsInitTargets.AdvGeoForm) {
      _init()
    } else {
      super.routeInitTarget(itg)
    }
  }

  /** Запуск инициализации текущего модуля. */
  private def _init(): Unit = {

    // Инициализировать хранилку ссылки на гифку прелоадера, т.к. тот будет стёрт входе react-рендера.
    LkPreLoader.PRELOADER_IMG_URL

    val module = wire[LkAdvGeoFormModule]

    val circuit = module.lkAdvGeoFormCircuit

    // Рендер всей формы:
    val formR = circuit.wrap(identity(_))( module.advGeoFormR.component.apply )
    val formTarget = VUtil.getElementById[HTMLDivElement]( AdvGeoConstants.REACT_FORM_TARGET_ID ).get
    formR.renderIntoDOM( formTarget )

    // Отдельно идёт рендер виджета цены PriceR:
    val priceR = circuit.wrap(_.adv.bill.price)( PriceR.component.apply )
    val priceTarget = VUtil.getElementById[HTMLDivElement]( AdvConstants.Price.OUTER_CONT_ID ).get
    priceR.renderIntoDOM( priceTarget )

  }

}
