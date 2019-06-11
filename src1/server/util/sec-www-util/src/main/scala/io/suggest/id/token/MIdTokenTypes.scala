package io.suggest.id.token

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.06.19 18:43
  * Description: Модель типов id-токенов, описывающих их назначение.
  */
object MIdTokenTypes extends StringEnum[MIdTokenType] {

  /** Токен проверки смс-кодом. */
  case object PhoneCheck extends MIdTokenType("p")

  override val values = findValues

}


sealed abstract class MIdTokenType(override val value: String) extends StringEnumEntry

object MIdTokenType {

  implicit def mIdTokenTypeJson: Format[MIdTokenType] =
    EnumeratumUtil.valueEnumEntryFormat( MIdTokenTypes )

  @inline implicit def univEq: UnivEq[MIdTokenType] = UnivEq.derive

}
