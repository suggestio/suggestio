package io.suggest.pwa.manifest

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.18 18:33
  * Description: Mobile application distribute platforms, related to web-apps.
  */
object MAppPlatforms extends StringEnum[MAppPlatform] {

  case object GooglePlay extends MAppPlatform("play")

  override def values = findValues

}


sealed abstract class MAppPlatform(override val value: String) extends StringEnumEntry {
  override final def toString = value
}


object MAppPlatform {

  @inline implicit def univEq: UnivEq[MAppPlatform] = UnivEq.derive

  implicit def MAPP_PLATFORM_FORMAT: Format[MAppPlatform] = {
    EnumeratumUtil.valueEnumEntryFormat( MAppPlatforms )
  }

}

