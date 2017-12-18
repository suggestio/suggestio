package io.suggest.msg

import io.suggest.bill.MPrice
import io.suggest.dt.MYmd
import io.suggest.i18n.MsgCodes
import io.suggest.sjs.common.dt.MYmdJs

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
      val d0 = d.getDay()
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
    Messages( MsgCodes`DayOfW.N.`( dowHelper(dow) ) )
  }

}
