package io.suggest.n2.bill.tariff.daily

import io.suggest.bill.{IMCurrency, MCurrency, MPrice}
import io.suggest.es.{IEsMappingProps, MappingDsl}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.model.PrefixedFn
import monocle.macros.GenLens

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.12.15 21:14
 * Description: Модель тарифа посуточного размещения на узле.
 */
object MTfDaily
  extends IEsMappingProps
{

  /** Названия полей. */
  object Fields {

    val CURRENCY_FN     = "currency"
    val CLAUSES_FN      = "clauses"
    val COMISSION_PC_FN = "comission"

    object Clauses extends PrefixedFn {
      override protected def _PARENT_FN = CLAUSES_FN
      def CAL_ID_FN = _fullFn( MDayClause.Fields.CALENDAR_ID_FN )
    }

  }


  import Fields._

  implicit val FORMAT: OFormat[MTfDaily] = (
    (__ \ CURRENCY_FN).format[MCurrency] and
    (__ \ CLAUSES_FN).format[Seq[MDayClause]]
      .inmap [Map[String, MDayClause]] (
        _.iterator.map { v => v.name -> v }.toMap,
        _.valuesIterator.toSeq
      ) and
    (__ \ COMISSION_PC_FN).formatNullable[Double]
  )(apply, unlift(unapply))


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.CURRENCY_FN -> FKeyWord.indexedJs,
      F.CLAUSES_FN  -> FObject.plain( MDayClause ),
      F.COMISSION_PC_FN -> FNumber(
        typ = DocFieldTypes.Double,
        index = someFalse,
      ),
    )
  }

  def currency = GenLens[MTfDaily](_.currency)
  def clauses = GenLens[MTfDaily](_.clauses)
  def comissionPc = GenLens[MTfDaily](_.comissionPc)

}


/**
 * Экземпляр тарифа посуточного размещения на узле.
 *
 * @param currency Валюта тарифа.
 * @param clauses Описание дней и их тарификации.
 *                Должна быть хотя бы одна кляуза без календаря, т.е. дефолтовая.
 * @param comissionPc Комиссия suggest.io за размещение.
 *                    Например: 1.0 означает 100% уходит в CBCA.
 *                    None означает значение по умолчанию. Изначально = 1.0, но не обязательно.
  */
case class MTfDaily(
                     override val currency      : MCurrency,
                     clauses                    : ClausesMap_t,
                     // TODO comissionPc: Need exact/predictable Int type (== percents or promille) or BigDecimal (with stricted fractional part), instead of inexact Double type.
                     comissionPc                : Option[Double] = None
                   )
  extends IMCurrency
{

  private def _err(msg: String) = throw new IllegalArgumentException(msg)

  private def defaultClauseOpt = clauses.valuesIterator.find(_.calId.isEmpty)
  def defaultClause = defaultClauseOpt.get

  if (clauses.isEmpty)
    _err("Clauses must be non-empty.")

  if ( defaultClauseOpt.isEmpty )
    _err("At least one clause must be default: calId must be None.")

  if ( clauses.valuesIterator.count(_.calId.isEmpty) > 1 )
    _err("Too many default clauses with empty calId field.")

  override def toString: String = {
    s"$currency(${clauses.valuesIterator.map(cl => MPrice.amountToReal(cl.amount, currency)).mkString(";")})-${comissionPc.fold("")(_.toString + "%%")}"
  }

  def calIdsIter: Iterator[String] = {
    clauses
      .valuesIterator
      .flatMap(_.calId)
  }
  def calIds: Set[String] = {
    calIdsIter.toSet
  }

}
