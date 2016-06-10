package models.mlu

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.mlu.{ILookupModes, MLookupModesConstants}
import io.suggest.model.menum.EnumQsb

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.05.16 22:39
  * Description: sc v2. Модель режимов выборки карточек по id опорной карточки.
  */
object MLookupModes extends EnumMaybeWithName with ILookupModes with EnumQsb {

  /** Класс элементов модели. */
  protected[this] abstract class Val(override val strId: String)
    extends super.Val(strId)
    with ValT
  {
    override def toString: String = strId

    def withBefore  : Boolean
    def withAfter   : Boolean
  }

  override type T = Val


  import MLookupModesConstants._

  override val Around : T = new Val( AROUND_ID ) {
    override def withBefore = true
    override def withAfter  = true
  }

  override val Before : T = new Val( BEFORE_ID ) {
    override def withBefore = true
    override def withAfter  = false
  }

  override val After  : T = new Val( AFTER_ID ) {
    override def withBefore = false
    override def withAfter  = true
  }

}
