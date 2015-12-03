package models.adv.gtag

import models.MNode
import models.adv.tpl.IAdvPricing

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

  /** Доступные для рендера периоды. */
  def advPeriodsAvail: Seq[String]

  /** Начальная отображаемая цена. */
  def price: IAdvPricing

}


case class MForAdTplArgs(
  override val mad              : MNode,
  override val producer         : MNode,
  override val form             : GtForm_t,
  override val advPeriodsAvail  : Seq[String],
  override val price            : IAdvPricing
)
  extends IForAdTplArgs
