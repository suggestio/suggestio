package io.suggest.n2.bill.tariff.daily

import io.suggest.bill.Amount_t
import io.suggest.es.{IEsMappingProps, MappingDsl}
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
object MDayClause
  extends IEsMappingProps
{

  object Fields {
    val NAME_FN   = "n"
    private[MDayClause] val DOUBLE_AMOUNT_FN = "am"
    val AMOUNT_FN = "m"
    val CAL_ID_FN = "cal"
  }


  import Fields._

  implicit val FORMAT: Format[MDayClause] = {
    val amountFmt = (__ \ AMOUNT_FN).format[Amount_t]
    // Изначально, суммы были в double, поэтому тут FALLBACK: TODO Удалить FALLBACK после resaveMany().
    val amountFallbackReads = amountFmt.orElse {
      (__ \ DOUBLE_AMOUNT_FN).read[Double]
        .map { x => (x * 100).toLong }
    }
    val amountFmt2 = OFormat( amountFallbackReads, amountFmt)

    (
      (__ \ NAME_FN).format[String] and
      amountFmt2 and
      (__ \ CAL_ID_FN).formatNullable[String]
    )(apply, unlift(unapply))
  }


  def clauses2map(clauses: MDayClause*): ClausesMap_t = {
    clauses2map1(clauses)
  }
  def clauses2map1(clauses: IterableOnce[MDayClause]): ClausesMap_t = {
    clauses
      .iterator
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


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.NAME_FN -> FText.notIndexedJs,
      F.AMOUNT_FN -> FNumber(
        typ   = DocFieldTypes.Long,
        index = someFalse,
      ),
      F.CAL_ID_FN -> FKeyWord.indexedJs,
    )
  }

}


case class MDayClause(
  name    : String,
  amount  : Amount_t,
  calId   : Option[String] = None
) {

  def withAmount(amount2: Amount_t) = copy(amount = amount2)

}
