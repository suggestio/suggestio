package io.suggest.util

import annotation.tailrec
import org.apache.commons.lang.StringEscapeUtils

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.03.13 16:12
 * Description: Библиотека функций для работы с текстом.
 */
object TextUtil {

  /**
   * Триграмизировать слово, переданное в виде char[]. Функция ориентирована на работы с выхлопами SAX, ибо они содержат
   * (char[], start, len) в качестве аргументов.
   * @param chars список символов. Для строки используется метод toCharArray.
   * @return вернуть список токенов типа char[].
   */
  def trgm_chars_std(chars:Array[Char], start:Int=0, len:Int, acc0:List[String] = List()) : List[String] = {
    // Нужен дополнительный элемент, описывающий пробел в начале слова.
    val acc1 = if (len > 1)
      Array(' ', chars(start), chars(start+1)).mkString :: acc0
    else
      acc0
    trgm_chars(chars, start, len, acc1)
  }

  def trgm_token_std(token:String) = trgm_chars_std(token.toCharArray, len = token.length)


  /**
   * Самая полная триграммизация, т.е. с дополнительным упором на начало токена.
   * @param chars буквы
   * @param start сдвиг от начала
   * @param len кол-во символов до конца
   * @return
   */
  def trgm_chars_full(chars:Array[Char], start:Int=0, len:Int) : List[String] = {
    // full-trgm - это дописать два пробела в начале для усиления начала слова
    if (len > 0) {
      val acc0 = List(
        Array(' ', ' ', chars(start)).mkString
      )
      trgm_chars_std(chars, start, len, acc0)
    } else
      List()
  }

  def trgm_token_full(token:String) = trgm_chars_full(token.toCharArray, len = token.length)


  /**
   * Триграммизация без учета начала слова, но с отметкой конца слова.
   * @param chars буквы
   * @param start начало
   * @param len длина
   * @return список триграмм
   */
  def trgm_chars_min_end(chars:Array[Char], start:Int=0, len:Int) : List[String] = {
    trgm_chars(chars, start, len, List())
  }

  def trgm_token_min_end(token:String) = trgm_chars_min_end(token.toCharArray, len = token.length)


  /**
   * Триграммизация без доп.учета начала-конца токена.
   * По сути тут чистый набор триграмм из символов в указанном диапазоне.
   * @param chars буквы
   * @param start начало
   * @param len длина
   * @return список триграмм
   */
  def trgm_chars_min(chars:Array[Char], start:Int=0, len:Int) : List[String] = {
    @tailrec def trgm_token_min1(_start:Int, _len:Int, _acc:List[String]) : List[String] = {
      if (_len > 2) {
        val _start1 = _start+1
        val _trgm   = Array(chars(_start), chars(_start1), chars(_start+2)).mkString
        trgm_token_min1(_start1, _len-1, _trgm::_acc)
      }
      else _acc
    }

    trgm_token_min1(start, len, List())
  }

  def trgm_token_min(token:String) = trgm_chars_min(token.toCharArray, len = token.length)


  /**
   * Обход токена в указанном диапазоне и генерация триграмм в аккамуляторе. Вернуть аккамулятор.
   * @param chars буковки
   * @param start начало букв
   * @param len длина
   * @param acc начальный аккамулятор
   * @return список триграмм
   */
  @tailrec protected final def trgm_chars(chars:Array[Char], start:Int, len:Int, acc:List[String]) : List[String] = {
    len match {
      // Выполняется обход слова, которое ещё не кончается.
      case l if l > 2 =>
        val start1 = start+1
        val trgm = Array(chars(start), chars(start1), chars(start+2)).mkString
        trgm_chars(chars, start1, len-1, trgm::acc)

      // Конец токена. Завершить рекурсию.
      case 2 => Array(chars(start), chars(start+1), ' ').mkString :: acc

      // какой-то предлог внезапный.
      case 1 => Array(' ', chars(start), ' ').mkString :: acc

      // Пустое барахло - остановить обход.
      case 0 => acc
    }
  }


  /**
   * Нормализация текста. Следует ещё nkfd делать.
   * @param text Юникодная строка для нормализации
   * @return Нормализованная строка.
   */
  def normalize(text:String) = text.toLowerCase

}
