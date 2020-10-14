package io.suggest.lk.nodes

import boopickle.Default._
import io.suggest.adn.edit.NodeEditConstants
import io.suggest.ble.eddystone.EddyStoneUtil
import io.suggest.text.StringUtil
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.{Validation, ValidationNel}
import scalaz.Validation.FlatMap._
import scalaz.syntax.apply._
import scalaz.std.option.optionSyntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.17 18:49
  * Description: Модель тела запроса создания/редактирования узла в списке узлов.
  */
object MLknNodeReq {

  /** BooPickler для инстансов модели. */
  implicit def mLknNodeReqPickler: Pickler[MLknNodeReq] = {
    generatePickler[MLknNodeReq]
  }


  def validateReq(req: MLknNodeReq, isEdit: Boolean): ValidationNel[String, MLknNodeReq] = {
    if (isEdit) {
      MLknNodeReq.validateEditReq(req)
    } else {
      MLknNodeReq.validateCreateReq(req)
    }
  }

  def validateEditReq(req: MLknNodeReq): ValidationNel[String, MLknNodeReq] = {
    val nameV = NodeEditConstants.Name.validateNodeName( req.name )
    // Нельзя редактировать id, хотя в модели запроса это поле присутствует.
    val nodeIdV = Validation.liftNel(req.id)( _.nonEmpty, "e.node.id.edit.not.impl" )
    (nameV |@| nodeIdV) { (_, _) => req }
  }

  def validateCreateReq(req: MLknNodeReq): ValidationNel[String, MLknNodeReq] = {
    val nameV = NodeEditConstants.Name.validateNodeName( req.name )
    val idV = req.id
      // На первом этапе можно добавлять только маячки, а они только с id.
      .toSuccessNel("e.node.id.missing")
      .flatMap { EddyStoneUtil.validateEddyStoneNodeId }
    (nameV |@| idV) { (_,_) => req }
  }

  @inline implicit def univEq: UnivEq[MLknNodeReq] = UnivEq.derive

  object Fields {
    def NAME = "n"
    def ID = "i"
  }

  implicit def mLknNodeReqFormat: OFormat[MLknNodeReq] = (
    (__ \ Fields.NAME).format[String] and
    (__ \ Fields.ID).formatNullable[String]
  )(apply, unlift(unapply))

}


/**
  * Класс модели реквеста добавления узла.
  *
  * @param name Имя узла.
  * @param id Идентификатор узла, если задан.
  */
case class MLknNodeReq(
                        name     : String,
                        id       : Option[String]
                      ) {

  override def toString: String = {
    StringUtil.toStringHelper( this, 128 ) { renderF =>
      val F = MLknNodeReq.Fields
      renderF( F.NAME )(name)
      id foreach renderF( F.ID )
    }
  }

}
