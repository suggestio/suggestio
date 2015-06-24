package io.suggest.sc.sjs.m.magent

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 10:21
 * Description: Статическая модель для описания данных приложения user-agent'а, в частности браузера
 * или мобильного приложения, в котором исполняется выдача.
 */
object MAgent {

  /** Размер и другие параметры экрана, доступные для рендера интерфейса. */
  @deprecated("FSM-MVM: Use MStData.screen instead.", "24.jun.2015")
  var availableScreen: MScreen = _


  override def toString = "MAgent"

}
