package io.suggest.sjs.common.model.mlu

import io.suggest.mlu.{MLookupModesConstants, MLookupModesLightT}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.06.16 22:29
  * Description: Реализация модели режимов lookup'а карточек для sjs.
  */
object MLookupModes extends MLookupModesLightT {

  protected[this] class Val(override val strId: String) extends ValT

  override type T = Val

  override val Around: T = new Val( MLookupModesConstants.AROUND_ID )
  override val Before: T = new Val( MLookupModesConstants.BEFORE_ID )
  override val After : T = new Val( MLookupModesConstants.AFTER_ID )

}
