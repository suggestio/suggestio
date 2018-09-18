package io.suggest.pwa.manifest

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.18 17:50
  * Description: Допустимые варианты поля [[MWebManifest]].display.
  */
object MPwaDisplayModes extends StringEnum[MPwaDisplayMode] {

  case object FullScreen extends MPwaDisplayMode("standalone")

  case object Standalone extends MPwaDisplayMode("fullscreen")

  case object MinimalUi extends MPwaDisplayMode("minimal-ui")

  case object Browser extends MPwaDisplayMode("browser")


  override def values = findValues

}


sealed abstract class MPwaDisplayMode(override val value: String)
  extends StringEnumEntry
  with Product
{
  override final def toString = value
}


object MPwaDisplayMode {

  @inline implicit def univEq: UnivEq[MPwaDisplayMode] = UnivEq.derive

  /** Поддержка play-json. */
  implicit def MPWA_DISPLAY_FORMAT: Format[MPwaDisplayMode] = {
    EnumeratumUtil.valueEnumEntryFormat( MPwaDisplayModes )
  }

}
