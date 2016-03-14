package models.adv.geo.tag

import models.MNode
import models.adv.IAdvForAdCommonTplArgs
import models.adv.price.IAdvPricing

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.11.15 15:05
  * Description: Модель аргументов шаблона [[views.html.lk.adv.gtag.forAdTpl]].
  */
trait IForAdTplArgs extends IAdvForAdCommonTplArgs {

  /** Экземпляр маппинга формы размещения карточки в теге с географией. */
  def form      : GtForm_t

  /** Доступные для рендера периоды. */
  def advPeriodsAvail: Seq[String]

}


case class MForAdTplArgs(
  override val mad              : MNode,
  override val producer         : MNode,
  override val form             : GtForm_t,
  override val advPeriodsAvail  : Seq[String],
  override val price            : IAdvPricing
)
  extends IForAdTplArgs
