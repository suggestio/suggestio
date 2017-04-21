package io.suggest.lk.adn.map.init

import io.suggest.adn.mapf.AdnMapFormConstants
import io.suggest.adv.AdvConstants
import io.suggest.lk.adn.map.LkAdnMapCircuit
import io.suggest.lk.adn.map.r.LamFormR
import io.suggest.lk.adv.r.PriceR
import io.suggest.sjs.common.controller.IInit
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.spa.LkPreLoader
import japgolly.scalajs.react.ReactDOM
import org.scalajs.dom.raw.HTMLDivElement

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
    // Основное тело формы:
    ReactDOM.render(
      element   = circuit.wrap(m => m)( LamFormR.apply ),
      container = VUtil.getElementByIdOrNull[HTMLDivElement]( AdnMapFormConstants.FORM_CONT_ID )
    )

    // Виджет стоимости на правой панели:
    ReactDOM.render(
      element   = circuit.wrap(_.price)(PriceR.apply),
      container = VUtil.getElementByIdOrNull[HTMLDivElement]( AdvConstants.Price.OUTER_CONT_ID )
    )

    // Запуск основного FSM формы.
    //val formFsm = new LamFormFsm
    //formFsm.start()

    // Инициализатор карты размещения.
    //val mapInitializer = new LkAdnMapInit
    //mapInitializer.init()

    // Инициализация остальной формы.
    //val initFsmF = IInitLayoutFsm.f(formFsm)
    //LamForm.find().foreach(initFsmF)
  }

}
