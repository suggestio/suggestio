package io.suggest.util

import annotation.tailrec
import scala.util.matching.Regex

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.03.13 16:12
 * Description: Библиотека функций для работы с текстом.
 */
object TextUtil {

  /** Если концентрация не-алфавитных символов выше этого уровня, то проводить патчинг слова. */
  val WORD_MISCHAR_THRESHOLD = 0.55F

  /**
   * Триграмизировать слово, переданное в виде char[]. Функция ориентирована на работы с выхлопами SAX, ибо они содержат
   * (char[], start, len) в качестве аргументов.
   * @param chars список символов. Для строки используется метод toCharArray.
   * @return вернуть список токенов типа char[].
   */
  def trgmCharsStd(chars:Array[Char], start:Int=0, len:Int, acc0:List[String] = List()) : List[String] = {
    // Нужен дополнительный элемент, описывающий пробел в начале слова.
    val acc1 = if (len > 1)
      Array(' ', chars(start), chars(start+1)).mkString :: acc0
    else
      acc0
    trgmChars(chars, start, len, acc1)
  }

  def trgmTokenStd(token:String) = trgmCharsStd(token.toCharArray, len = token.length)


  /**
   * Самая полная триграммизация, т.е. с дополнительным упором на начало токена.
   * @param chars буквы
   * @param start сдвиг от начала
   * @param len кол-во символов до конца
   * @return
   */
  def trgmCharsFull(chars:Array[Char], start:Int=0, len:Int) : List[String] = {
    // full-trgm - это дописать два пробела в начале для усиления начала слова
    if (len > 0) {
      val acc0 = List(
        Array(' ', ' ', chars(start)).mkString
      )
      trgmCharsStd(chars, start, len, acc0)
    } else
      List()
  }

  def trgmTokenFull(token:String) = trgmCharsFull(token.toCharArray, len = token.length)


  /**
   * Триграммизация без учета начала слова, но с отметкой конца слова.
   * @param chars буквы
   * @param start начало
   * @param len длина
   * @return список триграмм
   */
  def trgmCharsMinEnd(chars:Array[Char], start:Int=0, len:Int) : List[String] = {
    trgmChars(chars, start, len, List())
  }

  def trgmTokenMinEnd(token:String) = trgmCharsMinEnd(token.toCharArray, len = token.length)


  /**
   * Триграммизация без доп.учета начала-конца токена.
   * По сути тут чистый набор триграмм из символов в указанном диапазоне.
   * @param chars буквы
   * @param start начало
   * @param len длина
   * @return список триграмм
   */
  def trgmCharsMin(chars:Array[Char], start:Int=0, len:Int) : List[String] = {
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

  def trgmTokenMin(token:String) = trgmCharsMin(token.toCharArray, len = token.length)


  /**
   * Обход токена в указанном диапазоне и генерация триграмм в аккамуляторе. Вернуть аккамулятор.
   * @param chars буковки
   * @param start начало букв
   * @param len длина
   * @param acc начальный аккамулятор
   * @return список триграмм
   */
  @tailrec protected final def trgmChars(chars:Array[Char], start:Int, len:Int, acc:List[String]) : List[String] = {
    len match {
      // Выполняется обход слова, которое ещё не кончается.
      case l if l > 2 =>
        val start1 = start+1
        val trgm = Array(chars(start), chars(start1), chars(start+2)).mkString
        trgmChars(chars, start1, len-1, trgm::acc)

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
  def normalize(text: String) = text.toLowerCase


  /** Костыль для транляции скрытых символов визуальной транслитерации. Обычно ошибки в букве с,
    * которая русская и английская на на одной кнопке. */
  val untranslitInvisibleCharRu: PartialFunction[Char, Char] = {
    case 'c' => 'с'
    case 'C' => 'С'
    case 'a' => 'а'
    case 'e' => 'е'
    case 'o' => 'о'
    case 'p' => 'р'
    case 'B' => 'В'
    case 'r' => 'г'
    case 'R' => 'Я'  // Бывает, что "R" -- это русская "р".
    case 'b' => 'ь'
    case 'M' => 'М'
    case 'x' => 'х'
    case 'u' => 'и'
    case 'H' => 'Н'
    case 'T' => 'Т'
    case 'N' => 'И'
    case 'k' => 'к'
    case 'K' => 'К'
    case 'D' => 'Д'
    case 'y' => 'у'
    case 'Y' => 'У'
    case 'W' => 'Ш'
    case 'w' => 'ш'
    case '@' => 'а'
    // Видимые и преднамеренные замены букв.
    case 'Z' => 'З'
    case 'z' => 'з'
    // Костыль против поехавших расовых хохлов и некоторых особо упоротых змагаров
    case 'Ґ' => 'Г'
    case 'ґ' => 'г'
    // Цифры, каракули и т.д. => RU в необходимом регистре.
    case '3' => 'з'
    case '0' => 'О'
    case '6' => 'б'
    case '7' => 'T'
    // Когда будет поддержка разных наборов костылей, надо это выкинуть для возможности комбинирования.
    case ch  => ch
  }

  /** Костыль для отката скрытой визуальной транслитерации на english. */
  val untranslitInvisibleCharEn: PartialFunction[Char, Char] = {
    case 'с' => 'c'
    case 'С' => 'c'
    case 'Т' => 'Т'
    case 'о' => 'o'
    case 'О' => 'O'
    case 'a' => 'а'
    case 'А' => 'A'
    case 'е' => 'e'
    case 'Е' => 'E'
    case 'р' => 'p'
    case 'Р' => 'P'
    case 'Я' => 'R'
    case 'я' => 'R'
    case 'ш' => 'w'
    case 'Ш' => 'W'
    case 'у' => 'y'
    case 'У' => 'Y'
    case 'и' => 'u'
    case 'к' => 'k'
    case 'К' => 'K'
    case 'Н' => 'H'
    case 'в' => 'B'
    case 'В' => 'B'
    case 'З' => 'E'
    case 'М' => 'M'
    case 'И' => 'N'
    case 'г' => 'r'
    case 'ь' => 'b'
    // цифры
    case '0' => 'O'
    case '1' => 'l'
    case '3' => 'E'
    case '7' => 'T'
    // Усё
    case ch  => ch
  }

  private type TranslitMap_t = List[(Regex, PartialFunction[Char,Char])]
  private val translitInvMap: TranslitMap_t = List(
    "(?iu)[а-я]+".r -> untranslitInvisibleCharRu,
    "(?i)[a-z]+".r  -> untranslitInvisibleCharEn
  )

  /** Слово может иметь ошибочно (или намеренно) вставленные символы в иной раскладке, которые выглядят как натуральные.
    * Такое является особенно касается буквы c. */
  def fixMischaractersInWord(word: String): String = {
    detectAndApplyLangFix(word, translitInvMap)
  }

  @tailrec final def detectAndApplyLangFix(word: String, tMapRest: TranslitMap_t): String = {
    if (!tMapRest.isEmpty) {
      // Продолжаем обрабатывать карту фиксов.
      val (re, charFixer) = tMapRest.head
      val cc = countCharsForRe(word, re)
      val ccRel = cc.toFloat / word.length.toFloat
      val isMixChar = ccRel < 1.0F
      if (isMixChar && ccRel > WORD_MISCHAR_THRESHOLD) {
        // Что-то в слове не так
        word.map(charFixer)
      } else {
        // Этот словарь фиксов не относится к этому слову. Перейти к следующем фикс-словарику.
        detectAndApplyLangFix(word, tMapRest.tail)
      }
    } else {
      // Нечего исправлять. Возвращаем слово наверх "как есть".
      word
    }
  }

  def countCharsForRe(s: String, re:Regex): Int = {
    val matcher = re.pattern.matcher(s)
    var i = 0
    while(matcher.find()) {
      i += matcher.end() - matcher.start()
    }
    i
  }

}
