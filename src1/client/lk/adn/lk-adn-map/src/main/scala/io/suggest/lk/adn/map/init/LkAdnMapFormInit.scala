package io.suggest.lk.adn.map.init

import io.suggest.lk.adn.map.fsm.LamFormFsm
import io.suggest.lk.adn.map.vm.LamForm
import io.suggest.sjs.common.controller.IInit
import io.suggest.sjs.common.fsm.IInitLayoutFsm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.16 18:19
  * Description: Инициализатор формы размещения ADN-узла на карте в точке.
  */
class LkAdnMapFormInit extends IInit {

  /** Запуск инициализации формы размещения узла . */
  override def init(): Unit = {

    // Запуск основного FSM формы.
    val formFsm = new LamFormFsm
    formFsm.start()

    // Инициализатор карты размещения.
    val mapInitializer = new LkAdnMapInit
    mapInitializer.init()

    // Инициализация остальной формы.
    val initFsmF = IInitLayoutFsm.f(formFsm)
    LamForm.find().foreach(initFsmF)
  }

}
