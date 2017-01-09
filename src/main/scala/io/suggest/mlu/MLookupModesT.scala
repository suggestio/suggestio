package io.suggest.mlu

import io.suggest.common.menum.{ILightEnumeration, LightEnumeration, StrIdValT}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.05.16 18:58
  * Description: sc v2.
  * Заготовка для моделей режима запроса focused-карточек.
  */
object MLookupModesConstants {

  def AROUND_ID = "a"

  def BEFORE_ID = "b"

  def AFTER_ID  = "c"

}


trait ILookupModes extends ILightEnumeration with StrIdValT {

  /** Режим поиска элементов вокруг запрашиваемой карточки вместе с карточкой. */
  val Around: T

  /** Режим поиска предшествующих карточек. */
  val Before: T

  /** Режим поиска карточек после указанной. */
  val After: T

}


/** Заготовка минимального enum'а для sjs-моделей. */
trait MLookupModesLightT extends ILookupModes with LightEnumeration {

  override def maybeWithName(n: String): Option[T] = {
    if (n == After.strId) {
      Some(After)
    } else if (n == Before.strId) {
      Some(Before)
    } else if (n == Around.strId) {
      Some(Around)
    } else {
      None
    }
  }

}
