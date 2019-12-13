package io.suggest.dev

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.2019 21:59
  * Description: Модель ОСей, т.е. софтварных "платформ".
  */
object MOses extends StringEnum[MOs] {

  /** Google Android. */
  case object Android extends MOs("a")

  /** Apple iOS. */
  case object AppleIOs extends MOs("i")


  override def values = findValues

}


sealed abstract class MOs(override val value: String) extends StringEnumEntry

object MOs {

  @inline implicit def univEq: UnivEq[MOs] = UnivEq.derive

  implicit def osPlatformJson: Format[MOs] =
    EnumeratumUtil.valueEnumEntryFormat( MOses )

}
