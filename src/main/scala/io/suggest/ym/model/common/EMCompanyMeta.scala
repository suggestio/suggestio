package io.suggest.ym.model.common

import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model.{EsModel, EsModelPlayJsonT, EsModelStaticMutAkvT}
import io.suggest.util.SioEsUtil._
import org.joda.time.DateTime
import play.api.libs.json._
import java.{util => ju}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.07.14 10:09
 * Description: Метаданные компании.
 */
object EMCompanyMeta {

  val META_ESFN = "meta"

}


import EMCompanyMeta._


trait EMCompanyMetaStatic extends EsModelStaticMutAkvT {
  override type T <: EMCompanyMetaMut

  abstract override def generateMappingProps: List[DocField] = {
    val f = FieldObject(META_ESFN, enabled = true, properties = MCompanyMeta.generateMappingProps)
    f :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (META_ESFN, v: ju.Map[_,_])  =>
        acc.meta = MCompanyMeta.deserialize(v)
    }
  }
}


trait EMCompanyMeta extends EsModelPlayJsonT {
  override type T <: EMCompanyMeta
  def meta: MCompanyMeta

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc1 = super.writeJsonFields(acc)
    if (meta.nonEmpty)
      META_ESFN -> meta.toPlayJson :: acc1
    else
      acc1
  }
}

trait EMCompanyMetaMut extends EMCompanyMeta {
  override type T <: EMCompanyMetaMut
  var meta: MCompanyMeta
}



object MCompanyMeta {

  val NAME_ESFN             = "name"
  val DATE_CREATED_ESFN     = "dateCreated"
  val OFFICE_PHONES_ESFN    = "oph"
  val CALL_TIME_START_ESFN  = "cts"
  val CALL_TIME_END_ESFN    = "cte"

  def generateMappingProps: List[DocField] = {
    List(
      FieldString(NAME_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldDate(DATE_CREATED_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldString(OFFICE_PHONES_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldNumber(CALL_TIME_START_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = true, fieldType = DocFieldTypes.integer),
      FieldNumber(CALL_TIME_END_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = true, fieldType = DocFieldTypes.integer)
    )
  }

  /** Фунцкия-десериализатор сериализованного значения MCompanyMeta. */
  val deserialize: PartialFunction[Any, MCompanyMeta] = {
    case jmap: ju.Map[_,_] =>
      import EsModel.{stringParser, dateTimeParser, strListParser, intParser}
      MCompanyMeta(
        name          = stringParser(jmap get NAME_ESFN),
        dateCreated   = dateTimeParser(jmap get DATE_CREATED_ESFN),
        officePhones  = strListParser(jmap get OFFICE_PHONES_ESFN),
        callTimeStart = Option(jmap get CALL_TIME_START_ESFN).map(intParser),
        callTimeEnd   = Option(jmap get CALL_TIME_END_ESFN).map(intParser)
      )
  }

}

case class MCompanyMeta(
  name          : String,
  dateCreated   : DateTime = DateTime.now,
  officePhones  : List[String] = Nil,
  callTimeStart : Option[Int] = None,
  callTimeEnd   : Option[Int] = None
) {
  import MCompanyMeta._

  def nonEmpty: Boolean = {
    productIterator exists {
      case opt: Option[_]       => opt.isDefined
      case coll: Traversable[_] => coll.nonEmpty
      case s: String            => !s.isEmpty
      case _                    => true
    }
  }
  def isEmpty = !nonEmpty

  def toPlayJson: JsObject = {
    var acc: FieldsJsonAcc = List(
      NAME_ESFN         -> JsString(name),
      DATE_CREATED_ESFN -> EsModel.date2JsStr(dateCreated)
    )
    if (officePhones.nonEmpty) {
      val ophs = officePhones.map(JsString.apply)
      acc ::= OFFICE_PHONES_ESFN -> JsArray(ophs)
    }
    if (callTimeStart.isDefined)
      acc ::= CALL_TIME_START_ESFN -> JsNumber(callTimeStart.get)
    if (callTimeEnd.isDefined)
      acc ::= CALL_TIME_END_ESFN -> JsNumber(callTimeEnd.get)
    JsObject(acc)
  }

}
