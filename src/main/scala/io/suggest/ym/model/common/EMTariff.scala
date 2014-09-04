package io.suggest.ym.model.common

import io.suggest.model.{EsModel, EsModelPlayJsonT, EsModelStaticMutAkvT}
import io.suggest.util.SioEsUtil._
import io.suggest.model.EsModel.FieldsJsonAcc
import java.{util => ju, lang => jl}
import scala.collection.JavaConversions._
import TariffTypes.TariffType
import play.api.libs.json._
import org.joda.time.{DateTime, Period}
import org.joda.time.format.ISOPeriodFormat
import io.suggest.ym.parsers.Price
import java.util.Currency

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.04.14 18:13
 * Description: Поле с массивом объектов-тарифов, навешанных на узел рекламной сети.
 * Каждый тарифный объект содержит поля:
 * - id: некий порядковый номер тарифа. Нужен для идентификации тарифа в списке при
 *   редактировании/удалении оного.
 * - tType: тип тарифа (просто периодическое списание, периодическое списание по статистике
 *   просмотров/показов и т.д.).
 * - enabled = true | false.
 * - name: Отображаемое имя.
 * - tBody: Объект, содержащий какие-то параметры конкретного тарифа.
 */

object EMTariff {

  /** Название top-level поля. */
  val TARIFF_ESFN   = "tariff"

  /** Для идентификации тарифов в общем списке используется поле id. Тут - название этого поля. */
  val ID_ESFN       = "tid"
  val NAME_ESFN     = "name"
  val TTYPE_ESFN    = "tType"
  val ENABLED_ESFN  = "enabled"
  val TBODY_ESFN    = "tBody"

  /** Дата первого списания. Все остальные даты списания генерятся на основе этой даты и периода. */
  val DATE_FIRST_ESFN    = "dateStart"
  /** Название поля в котором записаны дата-время создания тарифа. */
  val DATE_CREATED_ESFN  = "dateCreated"
  /** Дата последнего изменения статуса enabled. */
  val DATE_STATUS_ESFN   = "dateStatus"
  /** Дата последнего изменения тарифа вообщем. */
  val DATE_MODIFIED_ESFN = "dateModified"
}


import EMTariff._

trait EMTariffStatic extends EsModelStaticMutAkvT {

  override type T <: EMTariff

  override abstract def generateMappingProps: List[DocField] = {
    FieldNestedObject(TARIFF_ESFN, enabled = true, properties = Seq(
      FieldNumber(ID_ESFN, index = FieldIndexingVariants.no, include_in_all = false, fieldType = DocFieldTypes.integer),
      FieldString(NAME_ESFN, FieldIndexingVariants.no, include_in_all = false),
      FieldString(TTYPE_ESFN, FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldBoolean(ENABLED_ESFN, FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldObject(TBODY_ESFN, enabled = false, properties = Nil),
      FieldDate(DATE_FIRST_ESFN, FieldIndexingVariants.no, include_in_all = false),
      FieldDate(DATE_CREATED_ESFN, FieldIndexingVariants.no, include_in_all = false),
      FieldDate(DATE_STATUS_ESFN, FieldIndexingVariants.no, include_in_all = false),
      FieldDate(DATE_MODIFIED_ESFN, FieldIndexingVariants.no, include_in_all = false)
    )) :: super.generateMappingProps
  }

  override abstract def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (TARIFF_ESFN, raw: jl.Iterable[_]) =>
        acc.tariffs = raw.foldLeft[List[Tariff]](Nil) {
          case (tAcc, tm: ju.Map[_, _]) => Tariff.deserialize(tm) :: tAcc
          case (tAcc, other) => ??? // подавляем warning при компиляции
        }
    }
  }
}


trait EMTariff extends EsModelPlayJsonT {
  override type T <: EMTariff

  var tariffs: List[Tariff]

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc1 = super.writeJsonFields(acc)
    if (tariffs.isEmpty) {
      acc1
    } else {
      val arrayElems = tariffs.map { _.toPlayJson }
      val tariffsJson = JsArray(arrayElems)
      TARIFF_ESFN -> tariffsJson :: acc1
    }
  }

  /** Выдать первый свободный id тарифа. */
  def nextTariffId: Int = {
    if (tariffs.isEmpty) {
      0
    } else {
      tariffs.maxBy(_.id).id + 1
    }
  }

  def getTariffById(id: Int): Option[Tariff] = {
    tariffs.find(_.id == id)
  }
}


object TariffTypes extends Enumeration {
  protected case class Val(name: String, longName: String) extends super.Val(name)

  type TariffType = Val

  implicit def value2val(x: Value) = x.asInstanceOf[Val]

  def maybeWithName(n: String): Option[TariffType] = {
    try {
      Some(withName(n))
    } catch {
      case ex: NoSuchElementException => None
    }
  }

  /** Периодическое списание денег. Абон.плата. */
  val PERIODICAL = Val("p", "period.fee")

  /** Периодическая проверка статистики и списание на основе кликов-просмотров и иных факторов. */
  //val STAT_PERIODICAL = Value("s")
}


object Tariff {

  val PERIOD_FORMATTER = ISOPeriodFormat.standard()

  /**
   * Десериализовать из карты-выхлопа jackson'а.
   * Самописный велосипед из-за проблем jackson'а в области scala.Enumeration.
   * @param tm Распарсенный сырой выхлоп jackson'а.
   * @return
   */
  def deserialize(tm: ju.Map[_,_]): Tariff = {
    val tid = EsModel.intParser(tm.get(ID_ESFN))
    val tName = tm.get(NAME_ESFN).toString
    val tType: TariffType = TariffTypes.withName(tm.get(TTYPE_ESFN).toString)
    val isEnabled: Boolean = EsModel.booleanParser(tm.get(ENABLED_ESFN))
    val tBody = tm.get(TBODY_ESFN) match {
      case tbodyRaw: ju.Map[_,_]  =>  TariffBody.deserialize(tType, tbodyRaw)
    }
    val dateStart    = EsModel.dateTimeParser(tm.get(DATE_FIRST_ESFN))
    val dateCreated  = EsModel.dateTimeParser(tm.get(DATE_CREATED_ESFN))
    val dateStatus   = EsModel.dateTimeParser(tm.get(DATE_STATUS_ESFN))
    val dateModified = Option(tm.get(DATE_MODIFIED_ESFN)).map(EsModel.dateTimeParser)
    Tariff(
      id    = tid,
      name  = tName,
      tType = tType,
      isEnabled     = isEnabled,
      tBody         = tBody,
      dateFirst     = dateStart,
      dateCreated   = dateCreated,
      dateStatus    = dateStatus,
      dateModified  = dateModified
    )
  }
}

import Tariff.PERIOD_FORMATTER

case class Tariff(
  var id            : Int,
  var name          : String,
  var tType         : TariffType,
  var isEnabled     : Boolean,
  var tBody         : TariffBody,
  var dateFirst     : DateTime,
  dateCreated       : DateTime = DateTime.now,
  var dateStatus    : DateTime = DateTime.now,
  var dateModified  : Option[DateTime] = None
) {
  def toPlayJson: JsObject = {
    var jsonFields: List[(String, JsValue)] = List(
      ID_ESFN      -> JsNumber(id),
      NAME_ESFN    -> JsString(name),
      TTYPE_ESFN   -> JsString(tType.toString),
      ENABLED_ESFN -> JsBoolean(isEnabled),
      TBODY_ESFN   -> tBody.toPlayJson,
      DATE_FIRST_ESFN   -> EsModel.date2JsStr(dateFirst),
      DATE_CREATED_ESFN -> EsModel.date2JsStr(dateCreated),
      DATE_STATUS_ESFN  -> EsModel.date2JsStr(dateStatus)
    )
    if (dateModified.isDefined)
      jsonFields ::= DATE_MODIFIED_ESFN -> EsModel.date2JsStr(dateModified.get)
    JsObject(jsonFields)
  }
}


object TariffBody {
  def deserialize(tType: TariffType, tbRaw: ju.Map[_,_]): TariffBody = {
    tType match {
      case TariffTypes.PERIODICAL => PeriodicalTariffBody.deserialize(tbRaw)
    }
  }
}


/** Абстрактное тело тарифа (данные ко конкретному тарифу). */
trait TariffBody {
  def toPlayJson: JsObject
  def describe: String
}


object PeriodicalTariffBody {
  val PERIOD_ESFN = "period"
  val AMOUNT_ESFN = "amount"
  val CURRENCY_CODE_ESFN = "currencyCode"
  
  def deserialize(tbRaw: ju.Map[_,_]): PeriodicalTariffBody = {
    val period = PERIOD_FORMATTER.parsePeriod(tbRaw.get(PERIOD_ESFN).toString)
    val amount: Float = EsModel.floatParser(tbRaw.get(AMOUNT_ESFN))
    val currencyCode = tbRaw.get(CURRENCY_CODE_ESFN).toString
    PeriodicalTariffBody(period, amount, currencyCode)
  }
}

case class PeriodicalTariffBody(period: Period, amount: Float, currencyCode: String)
  extends TariffBody {

  /** Конструктор с использование объекта price. */
  def this(period: Period, price: Price) = this(period, price.price, price.currency.getCurrencyCode)

  import PeriodicalTariffBody._

  def toPlayJson: JsObject = JsObject(Seq(
    PERIOD_ESFN        -> JsString(period.toString(PERIOD_FORMATTER)),
    AMOUNT_ESFN        -> JsNumber(amount),
    CURRENCY_CODE_ESFN -> JsString(currencyCode)
  ))

  def describe: String = {
    s"Every $period debit $amount $currencyCode."
  }

  def getPrice = Price(amount, Currency.getInstance(currencyCode))
}

