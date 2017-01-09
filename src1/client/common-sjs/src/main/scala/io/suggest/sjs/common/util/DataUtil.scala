package io.suggest.sjs.common.util

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.06.15 14:31
 * Description: Утиль для работы с данными.
 */
object DataUtil {

  /**
   * Извлечь целое число из строки, если оно там есть.
   * @param s Строка, содержащая число, "100px" например.
   * @param radix Основание системы счисления.
   * @see [[https://github.com/scala-js/scala-js/blob/master/javalanglib/src/main/scala/java/lang/Integer.scala#L65 По мотивам Integer.parseInt()]]
   * @return Целое, если найдено.
   */
  def extractInt(s: String, radix: Int = 10): Option[Int] = {
    Option(s)
      .filter { !_.isEmpty }
      .flatMap { s1 =>
        val res = js.Dynamic.global.parseInt(s1, radix)
          .asInstanceOf[scala.Double]
        if (res.isNaN || res > Integer.MAX_VALUE || res < Integer.MIN_VALUE) {
          None
        } else {
          Some(res.toInt)
        }
      }
  }

}
