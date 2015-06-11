package models.msc

import io.suggest.model.EnumMaybeWithName
import io.suggest.sc.focus.FocusedRenderModes

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.06.15 18:49
 * Description: Server-side реализация модели режимов рендера.
 */
object MFocRenderModes extends EnumMaybeWithName with FocusedRenderModes {

  /** Реализация экземпляра модели. */
  sealed protected[this] class Val(val strId: String)
    extends super.Val(strId)
    with ValT

  /** Экспортируемый наружу тип экземпляра. */
  override type T = Val

  override protected def instance(strId: String): T = new Val(strId)

  override val Normal = super.Normal
  override val Full   = super.Full

}
