package models.merr

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.model.menum.EnumJsonReadsT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 11:48
 * Description: Типы присылаемых ошибок.
 */
object MRemoteErrorTypes extends Enumeration with EnumMaybeWithName with EnumJsonReadsT {

  override type T = Value

  /** Ошибка возникла в выдаче. */
  val Showcase = Value : T

}

