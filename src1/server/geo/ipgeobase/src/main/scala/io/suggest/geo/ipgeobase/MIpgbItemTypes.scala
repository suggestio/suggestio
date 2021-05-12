package io.suggest.geo.ipgeobase

import enumeratum.values._
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.05.21 09:42
  * Description: Types model, to unify MIpRanges and MCities model.
  */

object MIpgbItemTypes extends StringEnum[MIpgbItemType] {

  /** IP Range type. Item contains ranged ip-addresses. */
  case object IpRange extends MIpgbItemType( "range" )

  /** City type. Item contains geo-info about city (town, village, other geo.point). */
  case object City extends MIpgbItemType( "city" )

  override def values = findValues

}


/** IP GeoBase stored item type value. */
sealed abstract class MIpgbItemType( override val value: String ) extends StringEnumEntry


object MIpgbItemType {

  @inline implicit def univEq: UnivEq[MIpgbItemType] = UnivEq.derive

  implicit def ipgbItemTypeJson: Format[MIpgbItemType] =
    EnumeratumUtil.valueEnumEntryFormat( MIpgbItemTypes )

}
