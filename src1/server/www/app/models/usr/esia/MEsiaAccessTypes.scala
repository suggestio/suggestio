package models.usr.esia

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.xplay.qsb.CrossQsBindable
import japgolly.univeq.UnivEq
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.03.19 16:06
  * Description: Модель запрашиваемых вариантов доступа.
  */
object MEsiaAccessTypes extends StringEnum[MEsiaAccessType] {

  /** Доступ к ресурсам требуется только при наличии владельца */
  case object Online extends MEsiaAccessType("online")

  /** Требуется иметь доступ к ресурсам и тогда,  когда  владелец  не  может  быть  вызван
    * (в этом случае выпускается маркер обновления).
    */
  case object Offline extends MEsiaAccessType("offline")


  override def values = findValues

}


sealed abstract class MEsiaAccessType(override val value: String) extends StringEnumEntry

object MEsiaAccessType {

  @inline implicit def univEq: UnivEq[MEsiaAccessType] = UnivEq.derive

  implicit def esiaAccessTypeQsb: CrossQsBindable[MEsiaAccessType] =
    EnumeratumJvmUtil.valueEnumQsb( MEsiaAccessTypes )

}
