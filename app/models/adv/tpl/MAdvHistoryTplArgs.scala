package models.adv.tpl

import models.{MAd, MNode}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.11.15 14:12
  * Description: Модель контейнера аргументов для вызова шаблона [[views.html.lk.adv.advHistoryTpl]].
  */
trait IAdvHistoryTplArgs {

  /** Размещаемая рекламная карточка. */
  def mad           : MAd

  /** Узел-продьюсер размещаемой карточки. */
  def producer      : MNode

  /** Данные списка транзакций. */
  def currAdvsArgs  : ICurrentAdvsTplArgs

}


case class MAdvHistoryTplArgs(
  override val mad           : MAd,
  override val producer      : MNode,
  override val currAdvsArgs  : ICurrentAdvsTplArgs
)
  extends IAdvHistoryTplArgs
