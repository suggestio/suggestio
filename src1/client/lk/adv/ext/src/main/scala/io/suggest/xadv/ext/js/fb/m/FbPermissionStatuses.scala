package io.suggest.xadv.ext.js.fb.m

import enumeratum.values.{StringEnum, StringEnumEntry}
import japgolly.univeq.UnivEq

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.15 16:25
 * Description:
 * Модель состояний выставленных у юзера пермишенов.
 * Изначально было только два варианта: заапрувлено и отказано, т.е. как бы булево.
 */
object FbPermissionStatuses extends StringEnum[FbPermissionStatus] {

  /** Юзер заапрувил разрешение. */
  case object Granted extends FbPermissionStatus("granted") {
    override def isGranted = true
  }

  /** Юзер вручную отключил разрешение. */
  case object Declined extends FbPermissionStatus("declined") {
    override def isGranted = false
  }

  override def values = findValues

}


sealed abstract class FbPermissionStatus(override val value: String) extends StringEnumEntry {

  /** Дан ли допуск к указанному разрешению? */
  def isGranted: Boolean

  /** Строка по мнению фейсбука. */
  @inline final def fbName = value

  override final def toString = fbName

}

object FbPermissionStatus {
  @inline implicit def univEq: UnivEq[FbPermissionStatus] = UnivEq.derive
}

