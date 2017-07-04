package io.suggest.model.n2.bill.tariff.daily

import io.suggest.es.model.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.12.15 22:07
 * Description: Модель описания тарифа для типов дней.
 * Появилась как качественное преобразование модели посуточных тарифов MmpDailyAdv, где была пачка однотипных полей:
 * например поля-ценники weekday, weekend, prime; а также поля-id календарей для полей-ценников.
 *
 * Эта модель описывает описание произвольных категорий дней года на основе цены и календаря.
 */
object MDayClause extends IGenEsMappingProps {

  object Fields {
    val NAME_FN   = "n"
    val AMOUNT_FN = "am"
    val CAL_ID_FN = "cal"
  }


  import Fields._

  implicit val FORMAT: Format[MDayClause] = (
    (__ \ NAME_FN).format[String] and
    (__ \ AMOUNT_FN).format[Double] and
    (__ \ CAL_ID_FN).formatNullable[String]
  )(apply, unlift(unapply))


  def clauses2map(clauses: MDayClause*): ClausesMap_t = {
    clauses2map1(clauses)
  }
  def clauses2map1(clauses: TraversableOnce[MDayClause]): ClausesMap_t = {
    clauses.toIterator
      .map { v => v.name -> v }
      .toMap
  }

  def clausesMap2list(clausesMap: ClausesMap_t): List[MDayClause] = {
    clausesMap
      .valuesIterator
      .toList
  }
  def clausesMap2seq(clausesMap: ClausesMap_t): Seq[MDayClause] = {
    clausesMap
      .valuesIterator
      .toSeq
  }

  import io.suggest.es.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldText(NAME_FN, index = false, include_in_all = false),
      FieldNumber(AMOUNT_FN, fieldType = DocFieldTypes.double, index = false, include_in_all = false),
      FieldKeyword(CAL_ID_FN, index = true, include_in_all = false)
    )
  }

}


case class MDayClause(
  name    : String,
  amount  : Double,
  calId   : Option[String] = None
) {

  def withAmount(amount2: Double) = copy(amount = amount2)

}
