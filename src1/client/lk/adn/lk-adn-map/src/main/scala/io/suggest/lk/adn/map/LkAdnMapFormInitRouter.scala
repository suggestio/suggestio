package io.suggest.lk.adn.map

import com.softwaremill.macwire._
import io.suggest.adn.mapf.AdnMapFormConstants
import io.suggest.adv.AdvConstants
import io.suggest.init.routed.InitRouter
import io.suggest.lk.adn.map.r.{LamFormR, LamPopupsR}
import io.suggest.lk.adv.r.PriceR
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.spa.LkPreLoader
import org.scalajs.dom.raw.HTMLDivElement
import japgolly.univeq._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.lk.adn.map.m.MRoot.MRootFastEq
import io.suggest.lk.adv.m.MPriceS.MPriceSFastEq
import io.suggest.lk.r.popup.PopupsContR

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.11.16 14:46
  * Description: Рендер динамических элементов формы размещения узла ADN на карте.
  * Сама форма и её статическое содержимое реднерится на сервере.
  * Так сделано для упрощения первого использования react-велосипеда в проекте.
  */

trait LkAdnMapFormInitRouter extends InitRouter {

  override protected def routeInitTarget(itg: MJsInitTarget): Unit = {
    if (itg ==* MJsInitTargets.AdnMapForm) {
      _init()
    } else {
      super.routeInitTarget(itg)
    }
  }


  /** Запуск инициализации формы размещения узла . */
  private def _init(): Unit = {

    // Инициализировать хранилку ссылки на гифку прелоадера, т.к. тот будет стёрт входе react-рендера.
    LkPreLoader.PRELOADER_IMG_URL

    val module = wire[LkAdnMapFormModule]

    // Инициализировать электросхему формы:
    val circuit = module.lkAdnMapCircuit

    // Припаять схему к html-вёрстке от сервера.
    val mrootRO = circuit.zoom(identity)

    // Основное тело формы:
    circuit
      .wrap(mrootRO)( module.lamFormR.component.apply )
      .renderIntoDOM(
        VUtil.getElementByIdOrNull[HTMLDivElement]( AdnMapFormConstants.FORM_CONT_ID )
      )

    // Виджет стоимости на правой панели:
    circuit
      .wrap(_.price)( PriceR.component.apply )
      .renderIntoDOM(
        VUtil.getElementByIdOrNull[HTMLDivElement]( AdvConstants.Price.OUTER_CONT_ID )
      )

    // Рендер контейнера обычных попапов.
    circuit
      .wrap(mrootRO)( module.lamPopupsR.component.apply )
      .renderIntoDOM(
        PopupsContR.initDocBody()
      )

  }

}
