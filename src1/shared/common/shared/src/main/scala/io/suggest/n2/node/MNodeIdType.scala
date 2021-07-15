package io.suggest.n2.node

import io.suggest.url.bind.{QsBindable, QsBinderF, QsUnbinderF}
import io.suggest.url.bind.QueryStringBindableUtil._
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** Container for node type with id. */
object MNodeIdType {

  object Fields {
    final def NODE_ID = "i"
    final def NODE_TYPE = "t"
  }

  implicit def nodeIdTypeJson: OFormat[MNodeIdType] = {
    val F = Fields
    (
      (__ \ F.NODE_ID).format[String] and
      (__ \ F.NODE_TYPE).format[MNodeType]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MNodeIdType] = UnivEq.derive


  /** Make compat instance for Bluetooth beacons.
    * Before 2021-06-24, only bluetooth radio-beacons was supported, and node types was not declared in API.
    */
  def bleBeaconFallback(beaconId: String): MNodeIdType =
    MNodeIdType( beaconId, MNodeTypes.RadioSource.BleBeacon )


  implicit def nodeIdTypeQsB(implicit
                             stringB: QsBindable[String],
                             nodeTypeB: QsBindable[MNodeType],
                            ): QsBindable[MNodeIdType] = {
    new QsBindable[MNodeIdType] {
      override def bindF: QsBinderF[MNodeIdType] = { (key, params) =>
        val F = MNodeIdType.Fields
        val k = key1F( key )

        (for {
          nodeIdE <- stringB.bindF( k(F.NODE_ID), params )
          nodeTypeE <- nodeTypeB.bindF( k(F.NODE_TYPE), params )
        } yield {
          for {
            nodeId <- nodeIdE
            nodeType <- nodeTypeE
          } yield {
            MNodeIdType(
              nodeId      = nodeId,
              nodeType    = nodeType,
            )
          }
        })
          .orElse {
            // 2021-06-24 Previosly, only nodeId was defined, and nodeType always was == BleBeacon. MNodeIdType model wasn't exist.
            // TODO Remove this code after some time: after mobile apps would be upgraded.
            for (nodeIdE <- stringB.bindF(key, params)) yield
              for (nodeId <- nodeIdE) yield
                MNodeIdType.bleBeaconFallback( nodeId )
          }
      }
      override def unbindF: QsUnbinderF[MNodeIdType] = { (key, value) =>
        val F = MNodeIdType.Fields
        val k = key1F( key )
        _mergeUnbinded1(
          stringB.unbindF( k(F.NODE_ID), value.nodeId ),
          nodeTypeB.unbindF( k(F.NODE_TYPE), value.nodeType ),
        )
      }
    }
  }

}


/** Node id with node type container.
  * May be used as isolated pointer to some node.
  *
  * @param nodeId id of node.
  * @param nodeType type of node.
  */
case class MNodeIdType(
                        nodeId      : String,
                        nodeType    : MNodeType,
                      )
