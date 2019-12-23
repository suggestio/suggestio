package io.suggest.xplay.qsb

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.10.16 16:15
  * Description: Утиль для внутренних реализаций QueryStringBindable.
  */
object QsbUtil {

  type IntOptE_t = Either[String, Option[Int]]

  /** Убедиться, что значение находится в указанном диапазоне или не задано вообще.
    *
    * @param intValueOptE Забинденное значение Option[Int].
    * @param minValue Минимальное допустимое значение.
    * @param maxValue Максимальное допустимое значение.
    * @return Выверенный результат бинда.
    */
  def ensureIntOptRange(intValueOptE: IntOptE_t, minValue: Int, maxValue: Int): IntOptE_t = {
    _checkOptValue( intValueOptE ) { intValue =>
      if (intValue >= minValue && intValue <= maxValue) {
        intValueOptE
      } else {
        Left("e.out.of.bounds")
      }
    }
  }


  type StrOptE_t = Either[String, Option[String]]

  /** Убедиться, что строковое опциональное значение по длине вписывается в допустимый диапазон.
    *
    * @param strValueOptE Забинденное опциональное строковое значение.
    * @param minLen Минимальная допустимая длина.
    * @param maxLen Максимальное допустимая длина.
    * @return Выверенный результат бинда.
    */
  def ensureStrOptLen(strValueOptE: StrOptE_t, minLen: Int, maxLen: Int): StrOptE_t = {
    _checkOptValue(strValueOptE) { strValue =>
      _checkStrLen(strValueOptE, strValue, minLen, maxLen = maxLen)
    }
  }


  private def _checkStrLen[T](ok: Either[String, T], strValue: String, minLen: Int, maxLen: Int): Either[String, T] = {
    val len = strValue.length
    if (len >= minLen && len <= maxLen) {
      ok
    } else {
      Left("e.str.len.max")
    }
  }


  /** Короткая и простая проверялка внутренносей опционального забинденного значения. */
  def _checkOptValue[T](valueOptE: Either[String, Option[T]])(f: T => Either[String, Option[T]]): Either[String, Option[T]] = {
    valueOptE.flatMap { valueOpt =>
      valueOpt.fold(valueOptE)(f)
    }
  }


  type StrE_t = Either[String, String]

  /** Убедится, что длина забинденной строки лежит в указанных пределах.
    *
    * @param strValueE Строковое значение.
    * @param minLen Минимальная допустимая длина.
    * @param maxLen Максимальная допустимая длина.
    * @return Выверенный результат бинда.
    */
  def ensureStrLen(strValueE: StrE_t, minLen: Int, maxLen: Int): StrE_t = {
    strValueE.flatMap { strValue =>
      _checkStrLen(strValueE, strValue, minLen = minLen, maxLen = maxLen)
    }
  }

}
