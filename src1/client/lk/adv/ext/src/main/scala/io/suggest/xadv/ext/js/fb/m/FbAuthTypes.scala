package io.suggest.xadv.ext.js.fb.m

import enumeratum.values.{StringEnum, StringEnumEntry}
import japgolly.univeq.UnivEq

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.03.15 10:08
 * Description: Режимы аутентификаций, передаваемые в FB.login().
 */
object FbAuthTypes extends StringEnum[FbAuthType] {

  /** Use this when re-requesting a declined permission. */
  case object ReRequest extends FbAuthType("rerequest")

  override def values = findValues

}


sealed abstract class FbAuthType(override val value: String) extends StringEnumEntry {
  @inline final def fbType = value
  override def toString = value
}


object FbAuthType {
  @inline implicit def univEq: UnivEq[FbAuthType] = UnivEq.derive
}
