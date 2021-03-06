package io.suggest.sc.model.dia.first

import diode.data.Pot
import enumeratum.{Enum, EnumEntry}
import io.suggest.perm.IPermissionState
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.19 12:53
  * Description: Модель страниц визарда.
  */
object MWzPhases extends Enum[MWzPhase] {

  /** Запуск, без заголовка и прочего. */
  case object Starting extends MWzPhase

  /** Описание запроса доступа к геолокации. */
  case object GeoLocPerm extends MWzPhase

  /** Описание запроса доступа к bluetooth. */
  case object BlueToothPerm extends MWzPhase

  /** Пермишшен нотификации. */
  case object NotificationPerm extends MWzPhase


  override def values = findValues

}


sealed abstract class MWzPhase extends EnumEntry

object MWzPhase {

  @inline implicit def univEq: UnivEq[MWzPhase] = UnivEq.derive

  implicit final class WzPhasePermsMapExt( private val perms: collection.Map[MWzPhase, Pot[IPermissionState]] ) extends AnyVal {

    /** @return true, when location permission is granted. */
    def hasLocationAccess: Boolean = {
      perms
        .get( MWzPhases.GeoLocPerm )
        .fold( false )( _.exists(_.isGranted) )
    }

  }

}