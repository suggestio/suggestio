package models.adv.direct

import models.MNode
import models.adv.IAdvForAdCommonTplArgs
import models.adv.price.MAdvPricing

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.11.15 14:06
  * Description: Модель контейнера аргументов вызова шаблона [[views.html.lk.adv.direct.advForAdTpl]].
  */
trait IAdvForAdTplArgs extends IAdvForAdCommonTplArgs {

  /** Данные формы размещения. */
  def formArgs  : IAdvFormTplArgs

}


case class MAdvForAdTplArgs(
  override val mad       : MNode,
  override val producer  : MNode,
  override val formArgs  : IAdvFormTplArgs,
  override val price     : MAdvPricing
)
  extends IAdvForAdTplArgs
