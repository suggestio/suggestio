package models.adv.geo.place

import models.MNode
import models.adv.IAdvForAdCommonTplArgs
import models.adv.price.IAdvPricing

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.16 15:19
  * Description: Интерфейсы и реализации моделей аргументов для вызова шаблона рендера
  * страницы/формы размещения в гео-месте.
  */


/** Интерфейс аргументов вызова шаблона только формы размещения карточки в месте. */
trait IAgpForAdFormTplArgs {

}


/** Интерфейс аргументов вызова шаблона страницы с формой размещения карточки в месте. */
trait IAgpForAdTplArgs extends IAgpForAdFormTplArgs with IAdvForAdCommonTplArgs {

}


/** Дефолтовая реализация [[IAgpForAdTplArgs]]. */
case class MAgpForAdTplArgs(
  override val mad              : MNode,
  override val producer         : MNode,
  override val price            : IAdvPricing
)
  extends IAgpForAdTplArgs
