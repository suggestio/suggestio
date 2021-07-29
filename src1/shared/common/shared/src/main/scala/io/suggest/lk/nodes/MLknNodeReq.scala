package io.suggest.lk.nodes

import io.suggest.adn.edit.NodeEditConstants
import io.suggest.n2.node.{MNodeType, MNodeTypes}
import io.suggest.netif.NetworkingUtil
import io.suggest.radio.MRadioSignal
import io.suggest.scalaz.ScalazUtil
import io.suggest.text.StringUtil
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.{Validation, ValidationNel}
import scalaz.syntax.apply._
import scalaz.syntax.validation._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.17 18:49
  * Description: Model of LkNodes Node form request body. Request used for creation/editing of single node.
  */
object MLknNodeReq {

  /** Do validation for MLknNodeReq.
    *
    * @param req Form data.
    * @param isEdit It is edit or create new?
    * @return Validation result with validated instance.
    */
  def validate(req: MLknNodeReq, isEdit: Boolean): ValidationNel[String, MLknNodeReq] = {
    val isCreate = !isEdit
    (
      // no String.trim(), because everything should be already sanitized and trimmed.
      NodeEditConstants.Name.validateNodeName( req.name ) |@|
      ScalazUtil
        // First, ensure optional id is mandatory for node editing:
        .liftNelOptMust( req.id, mustBeSome = isCreate, reallyMust = isCreate, error = "NodeID missing" )( _.successNel )
        // Do node-type-dependend checks:
        .andThen {
          case idOpt @ None if isEdit =>
            idOpt.successNel
          case idOpt =>
            req.nodeType match {
              case MNodeTypes.RadioSource.WifiAP =>
                ScalazUtil.liftNelSome( idOpt, "MAC-address expected" )( NetworkingUtil.validateMacAddress )
              case MNodeTypes.RadioSource.BleBeacon =>
                ScalazUtil.liftNelSome( idOpt, "EddyStone-UID expected" )( MRadioSignal.validateEddyStoneNodeId )
              case _ =>
                "ID validation unsupported for current node-type".failureNel[Option[String]]
            }
        } |@| {
        val ntypesAllowed =
          if (isEdit) MNodeTypes.lkNodesCanEdit
          else MNodeTypes.lkNodesUserCanCreate

        Validation.liftNel( req.nodeType )(
          !ntypesAllowed.contains[MNodeType](_),
          "Node-type unexpected"
        )
      }
    )( apply )
  }


  @inline implicit def univEq: UnivEq[MLknNodeReq] = UnivEq.derive

  object Fields {
    def NAME = "n"
    def ID = "i"
    def NODE_TYPE = "nodeType"
  }

  implicit def mLknNodeReqJson: OFormat[MLknNodeReq] = (
    (__ \ Fields.NAME).format[String] and
    (__ \ Fields.ID).formatNullable[String] and
    (__ \ Fields.NODE_TYPE).formatNullable[MNodeType]
      .inmap[MNodeType]( _ getOrElse MNodeTypes.RadioSource.BleBeacon, Some.apply )
  )(apply, unlift(unapply))

}


/** Class-container for model data of creation/editing single node inside LkNodes form.
  *
  * @param name Node name.
  * @param id Node uid, if needed (usually - must be defined).
  * @param nodeType Node type.
  *                 Some() for creation. Defaulted to BleBeacon.
  *                 Ignored for editing.
  */
final case class MLknNodeReq(
                              name          : String,
                              id            : Option[String],
                              nodeType      : MNodeType,
                            ) {

  override def toString: String = {
    StringUtil.toStringHelper( this, 128 ) { renderF =>
      val F = MLknNodeReq.Fields
      renderF( "" )( nodeType )
      id foreach renderF( F.ID )
      renderF( F.NAME )(name)
    }
  }

}
