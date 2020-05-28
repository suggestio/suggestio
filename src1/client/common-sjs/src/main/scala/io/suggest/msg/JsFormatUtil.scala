package io.suggest.msg

import io.suggest.bill.MPrice
import io.suggest.common.html.HtmlConstants
import io.suggest.dt.MYmd
import io.suggest.i18n.MsgCodes
import io.suggest.sjs.common.dt.MYmdJs
import japgolly.univeq._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.04.17 16:40
  * Description: Утиль для форматирования каких-либо объектов в строки, пригодные
  * для отображения обычному юзеру.
  */
object JsFormatUtil {

  /** Рендер ценника в строку. */
  def formatPrice(mprice: MPrice): String = {
    Messages(
      mprice.currency.i18nPriceCode,
      MPrice.amountStr(mprice)
    )
  }


  /** Отформатировать месяц в "января". */
  def formatMonth[T](month: T)(implicit helper: IMonthNumberHelper[T]): String = {
    Messages( MsgCodes.`ofMonth.N.`( helper(month) ) )
  }

  trait IMonthNumberHelper[T] {
    /** Конвертит исходное значение в номер месяца от 1 до 12 включительно. */
    def apply(x: T): Int
  }
  implicit object IntMonthNumberHelper extends IMonthNumberHelper[Int] {
    override def apply(x: Int): Int = x
  }
  implicit object YmdMonthNumberHelper extends IMonthNumberHelper[MYmd] {
    override def apply(x: MYmd): Int = x.month
  }


  /** Отформатировать день недели в "Понедельник". */
  def formatDayOfWeek[T](dow: T)(implicit dowHelper: IDayOfWeekHelper[T]): String = {
    Messages( MsgCodes.`DayOfWeek.N.`( dowHelper(dow) ) )
  }


  trait IDayOfWeekHelper[T] {
    /** Конверит исходное значение из произвольного в номер дня недели от 1 до 7 включительно. */
    def apply(x: T): Int
  }
  implicit object IntDayOfWeekHelper extends IDayOfWeekHelper[Int] {
    override def apply(x: Int): Int = x
  }
  implicit object JsDateDayOfWeekHelper extends IDayOfWeekHelper[js.Date] {
    override def apply(d: js.Date): Int = {
      val d0 = d.getDay().toInt
      if (d0 <= 0) 7 else d0
    }
  }
  implicit object YmdDayOfWeekHelper extends IDayOfWeekHelper[MYmd] {
    override def apply(x: MYmd): Int = {
      JsDateDayOfWeekHelper( MYmdJs.toJsDate(x) )
    }
  }


  /** Отформатировать день недели в короткое "пн", "вт", "ср" и т.д. */
  def formatDow[T](dow: T)(implicit dowHelper: IDayOfWeekHelper[T]): String = {
    Messages( MsgCodes.`DayOfW.N.`( dowHelper(dow) ) )
  }


  /** Отрендерить размер в удобных для человека единицах измерения: кило, мега, гига, тера.
    *
    * @param value Форматируемое число.
    * @param baseUnits Базовые единицы измерения.
    * @param jsMessages Рендерилка js messages.
    * @param use1024 Использовать 1024 вместо 1000 в качестве делителя.
    * @return Строка вида "1,4 К".
    */
  def formatKilMegGigTer(value: Double, baseUnits: String, use1024: Boolean)(implicit jsMessages: Messages): String = {
    val ten: Double = if (use1024) Math.pow(2, 10) else Math.pow(10, 3)

    val tera = Math.pow(ten, 4)
    val (unitPrefixMsgCode, dividerOpt) = if (value >= tera) {
      MsgCodes.`T._Tera` -> Some(tera)
    } else {
      val giga = Math.pow(ten, 3)
      if (value >= giga) {
        MsgCodes.`G._Giga` -> Some(giga)
      } else {
        val mega = Math.pow(ten, 2)
        if (value >= mega) {
          MsgCodes.`M._Mega` -> Some(mega)
        } else {
          val kilo = ten
          if (value >= kilo) {
            MsgCodes.`K._Kilo` -> Some(kilo)
          } else {
            "" -> None
          }
        }
      }
    }

    // Собираем значение вида 12.5423524
    val fracValue = dividerOpt
      .fold[Double](value)(value / _)

    val fracStr: String = if (fracValue >= 15) {
      fracValue.toInt.toString
    } else {
      val str0 = f"$fracValue%1.1f"
      val fracDelim = jsMessages( MsgCodes.`Number.frac.delim` )
      val dot = HtmlConstants.`.`
      if (fracDelim ==* dot) {
        str0
      } else {
        str0.replace(".", fracDelim )
      }
    }

    // Рендерим единицы измерения с префиксом.
    val units: String = if (baseUnits.isEmpty && unitPrefixMsgCode.isEmpty)
      baseUnits
    else if (unitPrefixMsgCode.isEmpty) {
      jsMessages( baseUnits )
    } else if (baseUnits.isEmpty) {
      jsMessages( unitPrefixMsgCode )
    } else {
      jsMessages(
        MsgCodes.`Prefixed0.metric.unit1`,
        jsMessages( unitPrefixMsgCode ),
        jsMessages( baseUnits ),
      )
    }

    // Рендерим число и единицы измерения.
    jsMessages( MsgCodes.`Number0.with.units1`, fracStr, units )
  }

}
