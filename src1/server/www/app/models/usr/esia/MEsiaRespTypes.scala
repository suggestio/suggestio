package models.usr.esia

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.xplay.qsb.CrossQsBindable
import japgolly.univeq.UnivEq
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.03.19 22:35
  * Description: Типы ответов ЕСИА.
  */
object MEsiaRespTypes extends StringEnum[MEsiaRespType] {

  /** Авторизационный код. */
  case object Code extends MEsiaRespType("code")

  override def values = findValues

}

sealed abstract class MEsiaRespType(override val value: String) extends StringEnumEntry

object MEsiaRespType {

  @inline implicit def univEq: UnivEq[MEsiaRespType] = UnivEq.derive

  implicit def esiaRespTypeQsb: CrossQsBindable[MEsiaRespType] =
    EnumeratumJvmUtil.valueEnumQsb( MEsiaRespTypes )

}
