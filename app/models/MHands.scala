package models

import io.suggest.common.MHandsBaseT
import io.suggest.common.menum.EnumMaybeWithName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.05.15 15:55
 * Description: Модель лева и права. Используется там где нужны эти два значения.
 * Называется модель моделью рук, но к самим рукам она отношения не имеет.
 */
object MHands extends Enumeration with EnumMaybeWithName with MHandsBaseT {

  /** Экземпляр модели. */
  protected abstract sealed class Val(override val strId: String)
    extends super.Val(strId)
    with super.ValT

  override type T = Val

  override val Left: T = new Val(LEFT_ID) with LeftT
  override val Right: T = new Val(RIGHT_ID) with RightT

}
