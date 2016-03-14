package models.adv

import models.adv.price.IAdvPricing
import models.mbase.{IProducer, IMad}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.16 16:12
  * Description: Общие интерфейсы аргументов для разных страниц размещения карточек.
  * Аргументы всегда имеют в наличии инстанс карточки и продьюсера.
  * Обычно присутсвует также инфа о стоимости услуги.
  */


/** Размещение, где оплата даже не предусмотрена, имеет аргументы, реализующие этот интерфейс: */
trait IAdvFreeForAdCommonTplArgs
  extends IMad
  with IProducer


/** Аргументы forAd-шаблона платного размещения реализуют этот общий интерфейс. */
trait IAdvForAdCommonTplArgs extends IAdvFreeForAdCommonTplArgs {

  /** Начальная отображаемая цена. */
  def price: IAdvPricing

}
