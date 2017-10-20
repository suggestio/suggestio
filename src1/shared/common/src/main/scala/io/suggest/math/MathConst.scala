package io.suggest.math

import io.suggest.common.html.HtmlConstants.`.`
import io.suggest.err.ErrorConstants

import scalaz.{Validation, ValidationNel}
import scalaz.syntax.apply._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.17 18:16
  * Description: Дополнительные математические константы.
  */
object MathConst {

  /** Проценты. */
  object Percents {

    /** Кол-во процентов в единице. */
    val PERCENTS_COUNT = 100

    /** Провалидировать, чтобы значение было между 0 и 100. */
    def validate_0_100(pct: Int, eMsgPrefix: => String = "pct"): ValidationNel[String, Int] = {
      Counts.validateMinMax(pct, 0, 100, eMsgPrefix)
    }

  }


  /** Количества. */
  object Counts {

    /** Провалидировать, чтобы значение было в указанном диапазоне. */
    def validateMinMax[T](v: T, min: T, max: T, eMsgPrefix: => String = "count")(implicit ev: Numeric[T]): ValidationNel[String, T] = {
      val errMsg = ErrorConstants.emsgF(eMsgPrefix + `.` + "too")
      (
        Validation.liftNel(v)(ev.lteq(_, min), errMsg("low")) |@|
        Validation.liftNel(v)(ev.gteq(_, max), errMsg("big"))
      ) { (_, _) => v }
    }

  }

}
