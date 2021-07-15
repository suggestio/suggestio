package io.suggest.lk.nodes

import io.suggest.ble.BeaconUtil
import io.suggest.es.model.MEsUuId
import io.suggest.n2.node.{MNodeIdType, MNodeTypes}
import io.suggest.netif.NetworkingUtil
import io.suggest.scalaz.{ScalazUtil, StringValidationNel}
import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.{Validation, ValidationNel}
import scalaz.std.list._
import scalaz.syntax.validation._
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

  /** JSON support. */
  implicit def lknBeaconsInfoReqJson: OFormat[MLknBeaconsScanReq] = {
    val F = Fields
    (
      {
        val path = (__ \ F.BEACON_UIDS)
        val formatNormal = path.format[List[MNodeIdType]]
        // Before 2021-06-24, published mobile apps supports only eddystone beacon ids.
        val readsCompat = formatNormal orElse {
          path
            .format[List[String]]
            .map(_.map { nodeId =>
              MNodeIdType.bleBeaconFallback( nodeId )
            })
        }
        OFormat( readsCompat, formatNormal )
      } and
      (__ \ F.AD_ID).formatNullable[String]
    )( apply, unlift(unapply) )
  }

  @inline implicit def univEq: UnivEq[MLknBeaconsScanReq] = UnivEq.derive


  /** Model validation.
    *
    * @param lknBcnReq Form data.
    * @return Validation result.
    */
  def validate(lknBcnReq: MLknBeaconsScanReq): ValidationNel[String, MLknBeaconsScanReq] = {
    (
      Validation.liftNel( lknBcnReq.beaconUids )(
        {bcns =>
          val len = bcns.size
          !((len > 0) && (len <= LkNodesConst.MAX_BEACONS_INFO_PER_REQ))
        },
        "Beacons array count invalid",
      )
        .andThen {
          ScalazUtil.validateAll(_) { nodeIdType =>
            validateNodeIdType( nodeIdType )
              .map( _ :: Nil )
              .orElse {
                println( getClass.getSimpleName + ": Dropped invalid node id/type: " + nodeIdType )
                List.empty.successNel
              }
          }
        } |@|
      ScalazUtil.liftNelOpt( lknBcnReq.adId ) { adId =>
        Validation.liftNel(adId)( !MEsUuId.isEsIdValid(_), "adId invalid" )
      }
    )( MLknBeaconsScanReq.apply )
  }


  private def _NODE_TYPE_UNSUPPORTED = "Unsupported node-type"


  /** Validate node-id and node-type in context of current model.
    * @param nodeIdType node id and type.
    * @return Validation result.
    */
  def validateNodeIdType(nodeIdType: MNodeIdType): StringValidationNel[MNodeIdType] = {
    (
      {
        val id = nodeIdType.nodeId.trim
        nodeIdType.nodeType match {
          case MNodeTypes.RadioSource.WifiAP =>
            NetworkingUtil.validateMacAddress( id )
          case MNodeTypes.RadioSource.BleBeacon =>
            BeaconUtil.EddyStone.validateBeaconId( id )
          case _ =>
            _NODE_TYPE_UNSUPPORTED.failureNel
        }
      } |@| {
        Validation.liftNel( nodeIdType.nodeType )(
          { ntype =>
            val isOk = MNodeTypes.RadioSource.children.exists { radioType =>
              ntype eqOrHasParent radioType
            }
            !isOk
          },
          _NODE_TYPE_UNSUPPORTED
        )
      }
    )( MNodeIdType.apply )
  }

}


/** Контейнер данных запроса инфы о маячках.
  *
  * @param beaconUids id интересующих маячков.
  * @param adId id текущей рекламной карточки, когда требуется возвращать содержимое MLknNode.adv .
  */
final case class MLknBeaconsScanReq(
                                     beaconUids   : List[MNodeIdType],
                                     adId         : Option[String],
                                   )
