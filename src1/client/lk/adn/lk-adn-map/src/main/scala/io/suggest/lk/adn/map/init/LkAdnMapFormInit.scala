package io.suggest.lk.adn.map.init

import io.suggest.adn.mapf.AdnMapFormConstants
import io.suggest.adv.AdvConstants
import io.suggest.lk.adn.map.LkAdnMapCircuit
import io.suggest.lk.adn.map.r.{LamFormR, LamPopupsR}
import io.suggest.lk.adv.r.PriceR
import io.suggest.lk.pop.PopupsContR
import io.suggest.sjs.common.controller.IInit
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.spa.LkPreLoader
import org.scalajs.dom.raw.HTMLDivElement
import japgolly.scalajs.react.vdom.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.16 18:19
  * Description: Инициализатор формы размещения ADN-узла на карте в точке.
  */
class LkAdnMapFormInit extends IInit {

  /** Запуск инициализации формы размещения узла . */
  override def init(): Unit = {

    // Инициализировать хранилку ссылки на гифку прелоадера, т.к. тот будет стёрт входе react-рендера.
    LkPreLoader.PRELOADER_IMG_URL

    // Инициализировать электросхему формы:
    val circuit = new LkAdnMapCircuit

    // Припаять схему к html-вёрстке от сервера.
    val mrootRO = circuit.zoom(m => m)

    // Основное тело формы:
    circuit
      .wrap(mrootRO)( LamFormR.apply )
      .renderIntoDOM(
        VUtil.getElementByIdOrNull[HTMLDivElement]( AdnMapFormConstants.FORM_CONT_ID )
      )

    // Виджет стоимости на правой панели:
    circuit
      .wrap(_.price)(PriceR.apply)
      .renderIntoDOM(
        VUtil.getElementByIdOrNull[HTMLDivElement]( AdvConstants.Price.OUTER_CONT_ID )
      )

    // Рендер контейнера обычных попапов.
    circuit
      .wrap(mrootRO)( LamPopupsR.apply )
      .renderIntoDOM(
        PopupsContR.initDocBody()
      )

  }

}
