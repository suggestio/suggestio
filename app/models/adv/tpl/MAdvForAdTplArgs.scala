package models.adv.tpl

import models.MNode
import models.adv.form.IAdvFormTplArgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.11.15 14:06
  * Description: Модель контейнера аргументов вызова шаблона [[views.html.lk.adv.advForAdTpl]].
  */
trait IAdvForAdTplArgs {

  /** Размещаемая рекламная карточка. */
  def mad       : MNode

  /** Узел-продьюсер размещаемой рекламной карточки. */
  def producer  : MNode

  /** Данные формы размещения. */
  def formArgs  : IAdvFormTplArgs

}


case class MAdvForAdTplArgs(
  override val mad       : MNode,
  override val producer  : MNode,
  override val formArgs  : IAdvFormTplArgs
)
  extends IAdvForAdTplArgs
