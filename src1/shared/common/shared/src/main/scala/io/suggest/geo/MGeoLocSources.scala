package io.suggest.geo

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import io.suggest.url.bind.QsBindable
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

object MGeoLocSources extends StringEnum[MGeoLocSource] {

  /** Coordinates from native api: from geo-satellites, BSS, etc. */
  case object NativeGeoLocApi extends MGeoLocSource("gps")

  /** Selected geo.point by user on the visual geo map. */
  case object GeoMap extends MGeoLocSource( "map" )

  /** Server-side detection via geo-ip services/databases. */
  case object GeoIP extends MGeoLocSource( "ip" )

  /** GeoLocation assigned/guessed from node-information data. */
  case object NodeInfo extends MGeoLocSource( "node" )

  override def values = findValues

}

sealed abstract class MGeoLocSource(override val value: String) extends StringEnumEntry

object MGeoLocSource {

  @inline implicit def univEq: UnivEq[MGeoLocSource] = UnivEq.derive

  implicit def geoLocSourceJson: Format[MGeoLocSource] =
    EnumeratumUtil.valueEnumEntryFormat( MGeoLocSources )

  implicit def geoLocSourceQsB(implicit intB: QsBindable[String]): QsBindable[MGeoLocSource] =
    EnumeratumUtil.qsBindable( MGeoLocSources )

}
