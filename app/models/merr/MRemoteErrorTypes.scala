package models.merr

import io.suggest.common.menum.EnumMaybeWithName
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 11:48
 * Description: Типы присылаемых ошибок.
 */
object MRemoteErrorTypes extends Enumeration with EnumMaybeWithName {

  override type T = Value

  /** Ошибка возникла в выдаче. */
  val Showcase = Value : T

  implicit def reads: Reads[T] = {
    __.read[String]
      .map { withName }
  }

}

