package io.suggest.sc.sjs.m.magent

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 17:22
 * Description: API доступа к данным и возможностям клиентской программы, исполняющей js-приложение
 * (браузер, компонент в приложении и т.д.).
 */
trait IAppStateAgent {

  /** Размер и другие параметры экрана, доступные для рендера интерфейса. */
  def availableScreen: IMScreen

}


case class MAppStateAgent(
  override val availableScreen: IMScreen
)
  extends IAppStateAgent
