package io.suggest.n2.node

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
