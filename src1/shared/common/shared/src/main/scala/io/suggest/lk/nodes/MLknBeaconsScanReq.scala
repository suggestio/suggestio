package io.suggest.lk.nodes

import io.suggest.es.model.MEsUuId
import io.suggest.scalaz.ScalazUtil
import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.{Validation, ValidationNel}
import scalaz.std.set._
import scalaz.syntax.apply._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.09.2020 13:06
  * Description: Контейнер данных запроса инфы по пачке маячков.
  */
object MLknBeaconsScanReq {

  object Fields {
    final def BEACON_UIDS = "b"
    final def AD_ID = "a"
  }

  implicit def lknBeaconsInfoReqJson: OFormat[MLknBeaconsScanReq] = {
    val F = Fields
    (
      (__ \ F.BEACON_UIDS).format[Set[String]] and
      (__ \ F.AD_ID).formatNullable[String]
    )( apply, unlift(unapply) )
  }

  @inline implicit def univEq: UnivEq[MLknBeaconsScanReq] = UnivEq.derive


  /** Валидация модели.
    *
    * @param lknBcnReq Реквест.
    * @return Результат валидации.
    */
  def validate(lknBcnReq: MLknBeaconsScanReq): ValidationNel[String, MLknBeaconsScanReq] = {
    (
      Validation.liftNel( lknBcnReq.beaconUids )(
        {bcns =>
          val len = bcns.size
          !((len > 0) && (len <= LkNodesConst.MAX_BEACONS_INFO_PER_REQ))
        },
        _e_beacons_ + "len",
      )
        .andThen {
          ScalazUtil.validateAll(_) { bcnUid =>
            Validation.liftNel( bcnUid )(
              !LkNodesConst.isBeaconIdValid(_),
              _e_beacons_ + "uid"
            )
              .map( Set.empty + _ )
          }
        } |@|
      ScalazUtil.liftNelOpt( lknBcnReq.adId ) { adId =>
        Validation.liftNel(adId)( !MEsUuId.isEsIdValid(_), _e_beacons_ + "adId" )
      }
    )( MLknBeaconsScanReq.apply )
  }

  private def _e_beacons_ = "e.beacons."

}


/** Контейнер данных запроса инфы о маячках.
  *
  * @param beaconUids id интересующих маячков.
  * @param adId id текущей рекламной карточки, когда требуется возвращать содержимое MLknNode.adv .
  */
final case class MLknBeaconsScanReq(
                                     beaconUids   : Set[String],
                                     adId         : Option[String],
                                   )
