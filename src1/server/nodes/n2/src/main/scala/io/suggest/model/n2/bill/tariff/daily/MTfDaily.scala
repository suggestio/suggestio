package io.suggest.model.n2.bill.tariff.daily

import io.suggest.bill.{IMCurrency, MCurrency}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.model.PrefixedFn

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.12.15 21:14
 * Description: Модель тарифа посуточного размещения на узле.
 */
object MTfDaily extends IGenEsMappingProps {

  /** Названия полей. */
  object Fields {

    val CURRENCY_FN     = "cc"
    val CLAUSES_FN      = "cl"
    val COMISSION_PC_FN = "com"

    object Clauses extends PrefixedFn {
      override protected def _PARENT_FN = CLAUSES_FN
      def CAL_ID_FN = _fullFn( MDayClause.Fields.CAL_ID_FN )
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


  import io.suggest.es.util.SioEsUtil._


  override def generateMappingProps: List[DocField] = {
    List(
      FieldKeyword(CURRENCY_FN, index = true, include_in_all = false),
      FieldObject(CLAUSES_FN, enabled = true, properties = MDayClause.generateMappingProps),
      FieldNumber(COMISSION_PC_FN, fieldType = DocFieldTypes.double, index = false, include_in_all = false)
    )
  }

}


/** Интерфейс для тарифного поля с описаловом тарифа размещения. */
trait ITfClauses extends IMCurrency {
  def clauses       : ClausesMap_t
}

/** Интерфейс для поля с %комиссией. */
trait ITfComissionPc extends IMCurrency {
  def comissionPc   : Option[Double]
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
  override val clauses       : ClausesMap_t,
  override val comissionPc   : Option[Double] = None
)
  extends ITfClauses
  with ITfComissionPc
{

  private def _err(msg: String) = throw new IllegalArgumentException(msg)

  if (clauses.isEmpty)
    _err("Clauses must be non-empty.")

  if ( !clauses.valuesIterator.exists(_.calId.isEmpty) )
    _err("At least one clause must be default: calId must be None.")

  if ( clauses.valuesIterator.count(_.calId.isEmpty) > 1 )
    _err("Too many default clauses with empty calId field.")

  override def toString: String = {
    s"$currency(${clauses.valuesIterator.map(_.amount).mkString(";")})-${comissionPc.fold("")(_ + "%%")}"
  }

  def calIdsIter: Iterator[String] = {
    clauses
      .valuesIterator
      .flatMap(_.calId)
  }
  def calIds: Set[String] = {
    calIdsIter.toSet
  }


  def withClauses(clauses2: ClausesMap_t) = copy(clauses = clauses2)
  def withComission(c: Option[Double]) = copy(comissionPc = c)

}
