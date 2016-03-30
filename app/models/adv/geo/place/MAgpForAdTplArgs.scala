package models.adv.geo.place

import models.MNode
import models.adv.IAdvForAdCommonTplArgs
import models.adv.form.IAdvForAdFormCommonTplArgs
import models.adv.price.IAdvPricing
import play.api.data.Form

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.16 15:19
  * Description: Интерфейсы и реализации моделей аргументов для вызова шаблона рендера
  * страницы/формы размещения в гео-месте.
  */


/** Интерфейс аргументов вызова шаблона страницы с формой размещения карточки в месте. */
trait IAgpForAdTplArgs
  extends IAdvForAdCommonTplArgs
  with IAdvForAdFormCommonTplArgs
{

  def form: Form[MAgpFormResult]

}


/** Дефолтовая реализация [[IAgpForAdTplArgs]]. */
case class MAgpForAdTplArgs(
  override val form             : Form[MAgpFormResult],
  override val mad              : MNode,
  override val producer         : MNode,
  override val price            : IAdvPricing
)
  extends IAgpForAdTplArgs
