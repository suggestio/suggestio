package models.adv.geo.tag

import models.MNode
import models.adv.IAdvForAdCommonTplArgs
import models.adv.form.IAdvForAdFormCommonTplArgs
import models.adv.price.IAdvPricing

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.11.15 15:05
  * Description: Модель аргументов шаблона [[views.html.lk.adv.geo.tag.AgtForAdTpl]].
  */
trait IForAdTplArgs extends IAdvForAdCommonTplArgs with IAdvForAdFormCommonTplArgs {

  /** Экземпляр маппинга формы размещения карточки в теге с географией. */
  def form      : AgtForm_t

}


case class MForAdTplArgs(
  override val mad              : MNode,
  override val producer         : MNode,
  override val form             : AgtForm_t,
  override val price            : IAdvPricing
)
  extends IForAdTplArgs
