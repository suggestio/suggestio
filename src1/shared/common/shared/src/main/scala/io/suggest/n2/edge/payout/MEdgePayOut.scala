package io.suggest.n2.edge.payout

import io.suggest.es.{IEsMappingProps, MappingDsl}
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens

object MEdgePayOut extends IEsMappingProps {

  @inline implicit def univEq: UnivEq[MEdgePayOut] = UnivEq.derive

  object Fields {
    final def TYPE = "type"
    final def DATA = "data"
  }

  implicit def edgePayoutJson: OFormat[MEdgePayOut] = {
    val F = Fields
    (
      (__ \ F.TYPE).format[MEdgePayOutType] and
      (__ \ F.DATA).format[JsObject]
    )(apply, unlift(unapply))
  }

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.TYPE -> FKeyWord.indexedJs,
      F.DATA -> FObject.disabled,
    )
  }

  def poType = GenLens[MEdgePayOut](_.poType)
  def data = GenLens[MEdgePayOut](_.data)

}


/** Container of single payout mechanism information.
  * Possible id# of current element may be defined in edge.doc.id.
  * This information is filled and processed by payment system implementations.
  *
  * @param poType Type of internal representation. Primarily may be used for information & statistics.
  * @param data Payment-system internal data.
  *             MEdgeInfo.paySystem should be defined.
  */
case class MEdgePayOut(
                        poType             : MEdgePayOutType,
                        data            : JsObject,
                      )
