package models.usr.esia

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.{EnumeratumJvmUtil, EnumeratumUtil}
import japgolly.univeq.UnivEq
import play.api.libs.json.Format
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.03.19 10:54
  * Description: Типы запрашиваемых маркеров доступа (access token).
  */
object MEsiaTokenTypes extends StringEnum[MEsiaTokenType] {

  /** в  настоящее  время  ЕСИА  поддерживает только значение “Bearer”. */
  case object Bearer extends MEsiaTokenType("Bearer")


  override def values = findValues

}


sealed abstract class MEsiaTokenType(override val value: String) extends StringEnumEntry

object MEsiaTokenType {

  @inline implicit def univEq: UnivEq[MEsiaTokenType] = UnivEq.derive

  implicit def esiaTokenTypeQsb: QueryStringBindable[MEsiaTokenType] =
    EnumeratumJvmUtil.valueEnumQsb( MEsiaTokenTypes )

  implicit def esiaTokenTypeFormat: Format[MEsiaTokenType] =
    EnumeratumUtil.valueEnumEntryFormat( MEsiaTokenTypes )

}
