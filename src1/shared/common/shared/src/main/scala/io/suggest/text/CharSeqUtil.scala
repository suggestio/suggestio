package io.suggest.text

import io.suggest.err.ErrorConstants

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.09.17 13:04
  * Description: Утиль для работы с Char Sequences, частным случаем которых являются строки.
  */
object CharSeqUtil {

  /** O(1) сборка view'а, который с около-нулевым оверхедом описывает
    * часть другой CharSequence от beginIndex до конца. */
  def view(source: CharSequence, beginIndex: Int): CharSeqView = {
    view(source, beginIndex = beginIndex, endIndex = source.length())
  }

  /** O(1) сборка view'а, который с около-нулевым оверхедом описывает
    * часть другой CharSequence от beginIndex до endIndex. */
  def view(source: CharSequence, beginIndex: Int, endIndex: Int): CharSeqView = {
    new CharSeqView(source, beginIndex = beginIndex, endIndex = endIndex)
  }

}


/** Zero-copy подстрока с помощью CharSequence.
  *
  * @param source Исходная строка.
  * @param beginIndex Начальный индекс.
  * @param endIndex Конечный индекс.
  */
class CharSeqView(
                   source      : CharSequence,
                   beginIndex  : Int,
                   endIndex    : Int
                 )
  extends CharSequence {

  ErrorConstants.assertArg( beginIndex >= 0 )
  ErrorConstants.assertArg( endIndex > beginIndex )
  ErrorConstants.assertArg( source.length() >= endIndex )

  /** O(1) длина char-последовательсти. */
  override def length: Int = {
    endIndex - beginIndex
  }

  /** O(1) сборка sub-view'а, который тоже [[CharSeqView]]. */
  override def subSequence(start2: Int, end2: Int): CharSeqView = {
    CharSeqUtil.view(this, start2, end2)
  }

  /** O(1) чтение одного Char'а. */
  override def charAt(index: Int): Char = {
    source.charAt(index + beginIndex)
  }

  /** O(length) Неспешная сборка в строку (с использованием методов выше) для всяких дебажных и тестовых нужд. */
  override lazy val toString: String = {
    val arr = Array.ofDim[Char](length)
    for (index <- 0 until length)
      arr(index) = charAt(index)
    new String(arr)
  }

}
