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
object MAppDistributors extends StringEnum[MAppDistributor] {

  case object GooglePlay extends MAppDistributor("play")

  case object AppleITunes extends MAppDistributor("itunes")


  override def values = findValues

}


sealed abstract class MAppDistributor(override val value: String) extends StringEnumEntry {
  override final def toString = value
}


object MAppDistributor {

  @inline implicit def univEq: UnivEq[MAppDistributor] = UnivEq.derive

  implicit def appPlatformJson: Format[MAppDistributor] = {
    EnumeratumUtil.valueEnumEntryFormat( MAppDistributors )
  }

}

