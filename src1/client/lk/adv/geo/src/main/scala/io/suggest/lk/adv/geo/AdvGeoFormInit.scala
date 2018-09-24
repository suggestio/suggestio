package io.suggest.lk.adv.geo

import io.suggest.adv.AdvConstants
import io.suggest.adv.geo.AdvGeoConstants
import io.suggest.lk.adv.geo.m.MRoot.MRootFastEq
import io.suggest.lk.adv.m.MPriceS.MPriceSFastEq
import io.suggest.lk.adv.geo.m.MPopupsS.MPopupsFastEq
import io.suggest.lk.adv.geo.r.AdvGeoFormR
import io.suggest.lk.adv.geo.r.pop.AdvGeoPopupsR
import io.suggest.lk.adv.r.PriceR
import io.suggest.lk.pop.PopupsContR
import io.suggest.sjs.common.controller.{IInit, InitRouter}
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.spa.LkPreLoader
import org.scalajs.dom.raw.HTMLDivElement
import japgolly.scalajs.react.vdom.Implicits._
import japgolly.univeq._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 10:24
 * Description: Инициализация формы размещения в геотегах.
 */
trait AdvGeoFormInitRouter extends InitRouter {

  override protected def routeInitTarget(itg: MJsInitTarget): Unit = {
    if (itg ==* MJsInitTargets.AdvGeoForm) {
      new AdvGeoFormInit()
        .init()
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
    LkPreLoader.PRELOADER_IMG_URL

    val circuit = LkAdvGeoFormCircuit

    // Рендер всей формы:
    val formR = circuit.wrap(m => m)(AdvGeoFormR.apply)
    val formTarget = VUtil.getElementByIdOrNull[HTMLDivElement]( AdvGeoConstants.REACT_FORM_TARGET_ID )
    formR.renderIntoDOM( formTarget )

    // Отдельно идёт рендер виджета цены PriceR:
    val priceR = circuit.wrap(_.bill.price)(PriceR.apply)
    val priceTarget = VUtil.getElementByIdOrNull[HTMLDivElement]( AdvConstants.Price.OUTER_CONT_ID )
    priceR.renderIntoDOM( priceTarget )

    // Рендер контейнера попапов.
    val popsContR = circuit.wrap(_.popups)( AdvGeoPopupsR.apply )
    val popsContTarget = PopupsContR.initDocBody()
    popsContR.renderIntoDOM( popsContTarget )

  }

}
