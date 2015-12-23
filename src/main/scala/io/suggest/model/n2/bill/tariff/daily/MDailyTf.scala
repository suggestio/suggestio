package io.suggest.model.n2.bill.tariff.daily

import io.suggest.model.es.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.12.15 21:14
 * Description: Модель тарифа посуточного размещения на узле.
 */
object MDailyTf extends IGenEsMappingProps {

  val CURRENCY_CODE_FN  = "cc"
  val CLAUSES_FN        = "cl"
  val COMISSION_PC_FN   = "com"


  implicit val FORMAT: OFormat[MDailyTf] = (
    (__ \ CURRENCY_CODE_FN).format[String] and
    (__ \ CLAUSES_FN).format[Seq[MDayClause]]
      .inmap [Map[String, MDayClause]] (
        _.iterator.map { v => v.name -> v }.toMap,
        _.valuesIterator.toSeq
      ) and
    (__ \ COMISSION_PC_FN).formatNullable[Double]
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._


  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(CURRENCY_CODE_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldObject(CLAUSES_FN, enabled = true, properties = MDayClause.generateMappingProps),
      FieldNumber(COMISSION_PC_FN, fieldType = DocFieldTypes.double, index = FieldIndexingVariants.no, include_in_all = false)
    )
  }

}


/**
 * Экземпляр тарифа посуточного размещения на узле.
 * @param currencyCode Валюта тарифа.
 * @param clauses Описание дней и их тарификации.
 *                Должна быть хотя бы одна кляуза без календаря, т.е. дефолтовая.
 * @param comissionPc Комиссия suggest.io за размещение.
 *                    1.0 означает 100% уходит в CBCA.
 *                    None означает 1.0
 */
case class MDailyTf(
  currencyCode  : String,
  clauses       : Map[String, MDayClause],
  comissionPc   : Option[Double] = None
) {

  private def _err(msg: String) = throw new IllegalArgumentException(msg)

  if (clauses.isEmpty)
    _err("Clauses must be non-empty.")

  if ( !clauses.valuesIterator.exists(_.calId.isEmpty) )
    _err("At least one clause must be default: calId must be None.")

  if ( clauses.valuesIterator.count(_.calId.isEmpty) > 1 )
    _err("Too many default clauses with empty calId field.")

}
