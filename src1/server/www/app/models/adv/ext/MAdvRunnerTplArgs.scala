package models.adv.ext

import io.suggest.model.n2.node.MNode
import models.adv.MExtAdvQs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.11.15 13:52
  * Description: Модель контейнера с аргументами вызова шаблона [[views.html.lk.adv.ext.advRunnerTpl]].
  */
trait IAdvRunnerTplArgs {

  /** Контейнер параметров для вызова websocket'а. */
  def wsCallArgs  : MExtAdvQs

  /** Размещаемая рекламная карточка. */
  def mad         : MNode

  /** Текущий узел, с которого происходит размещение карточки. */
  def mnode       : MNode

}


case class MAdvRunnerTplArgs(
  override val wsCallArgs  : MExtAdvQs,
  override val mad         : MNode,
  override val mnode       : MNode
)
  extends IAdvRunnerTplArgs
