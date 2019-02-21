package models.maps.umap

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.03.16 15:16
  */
object FeatureTypes extends StringEnum[FeatureType] {

  case object Feature extends FeatureType("Feature")

  case object FeatureCollection extends FeatureType("FeatureCollection")


  override def values = findValues

}


sealed abstract class FeatureType(override val value: String) extends StringEnumEntry

object FeatureType {

  implicit def featureTypeFormat: Format[FeatureType] =
    EnumeratumUtil.valueEnumEntryFormat( FeatureTypes )

  @inline implicit def univEq: UnivEq[FeatureType] = UnivEq.derive

}
