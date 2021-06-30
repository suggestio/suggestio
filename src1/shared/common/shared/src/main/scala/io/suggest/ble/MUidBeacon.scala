package io.suggest.ble

import io.suggest.n2.node.MNodeIdType
import io.suggest.primo.id.IId
import io.suggest.text.StringUtil
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:26
  * Description: Модель даных по одному UID-маячку.
  */
object MUidBeacon {

  object Fields {
    final def UID_FN             = "a"
    final def DISTANCE_CM_FN     = "g"
  }


  /** JSON support. Primarily, for client-side js-router URL query string serializing. */
  implicit def MUID_BEACON_FORMAT: OFormat[MUidBeacon] = {
    val F = Fields
    (
      {
        val jsonPath = (__ \ F.UID_FN)
        val formatNormal = jsonPath.format[MNodeIdType]
        // TODO 2021-06-25 Fallback for currently installed mobile apps. Remove this after installed apps upgrade.
        val readsFallback = formatNormal orElse {
          jsonPath
            .read[String]
            .map( MNodeIdType.bleBeaconFallback )
        }
        OFormat( readsFallback, formatNormal )
      } and
      (__ \ F.DISTANCE_CM_FN).formatNullable[Int]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MUidBeacon] = UnivEq.derive

}


/**
  * Класс для инстансов модели с инфой о наблюдаемом в эфире BLE-маячке.
  *
  * @param node Info about beacon node id/type.
  * @param distanceCm Расстояние в сантиметрах, если известно.
  */
final case class MUidBeacon(
                             node                 : MNodeIdType,
                             distanceCm           : Option[Int]       = None,
                           )
  extends IId[String]
{

  override def id = node.nodeId

  override def toString: String = {
    StringUtil.toStringHelper( this ) { renderF =>
      val emptyStr = ""
      renderF( emptyStr )( node )
      distanceCm foreach renderF( emptyStr )
    }
  }

}
