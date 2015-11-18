package models.adv.gtag

import models.MNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.11.15 15:05
  * Description: Модель аргументов шаблона [[views.html.lk.adv.gtag.forAdTpl]].
  */
trait IForAdTplArgs {

  /** Размещаяемая рекламная карточка. */
  def mad       : MNode

  /** Продьюсер карточки. */
  def producer  : MNode

  /** Экземпляр маппинга формы размещения карточки в теге с географией. */
  def form      : GtForm_t

}


case class MForAdTplArgs(
  override val mad        : MNode,
  override val producer   : MNode,
  override val form       : GtForm_t
)
  extends IForAdTplArgs
