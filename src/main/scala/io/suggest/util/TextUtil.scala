package io.suggest.util

import annotation.tailrec

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
  def trgmCharsStd(chars:Array[Char], start:Int=0, len:Int, acc0:List[String] = Nil) : List[String] = {
    // Нужен дополнительный элемент, описывающий пробел в начале слова.
    val acc1 = if (len > 1) {
      val chArr = Array(' ', chars(start), chars(start+1))
      new String(chArr) :: acc0
    } else {
      acc0
    }
    trgmChars(chars, start, len, acc1)
  }

  def trgmTokenStd(token:String, acc0:List[String] = Nil) = trgmCharsStd(token.toCharArray, len=token.length, acc0=acc0)


  /**
   * Самая полная триграммизация, т.е. с дополнительным упором на начало токена.
   * @param chars буквы
   * @param start сдвиг от начала
   * @param len кол-во символов до конца
   * @param acc0 Необязательный начальный аккамулятор токенов.
   * @return Список trgm-токенов в обратном порядке (самый левый токен в самом конце списка).
   */
  def trgmCharsFull(chars:Array[Char], start:Int=0, len:Int, acc0:List[String] = Nil) : List[String] = {
    // full-trgm - это дописать два пробела в начале для усиления начала слова
    if (len > 0) {
      val chArr = Array(' ', ' ', chars(start))
      val acc1 = new String(chArr) :: acc0
      trgmCharsStd(chars, start, len, acc1)
    } else {
      Nil
    }
  }

  /**
   * Полная триграммизация строки. По сути - тут враппер над [[io.suggest.util.TextUtil.trgmCharsFull]].
   * @param token Исходная строка, подлежащая перегонке в триграммы. Обычно это слово-токен, т.к. триграммизация строки
   *              с несколькими словами через эту функцию имеет мало смысла.
   * @param acc0 Необязательный начальный аккамулятор.
   * @return Список trgm-токенов в обратном порядке.
   */
  def trgmTokenFull(token:String, acc0:List[String] = Nil) = {
    trgmCharsFull(token.toCharArray, len=token.length, acc0=acc0)
  }


  /**
   * Триграммизация без учета начала слова, но с отметкой конца слова.
   * @param chars буквы
   * @param start начало
   * @param len длина
   * @return список триграмм
   */
  def trgmCharsMinEnd(chars:Array[Char], start:Int=0, len:Int, acc0:List[String] = Nil) : List[String] = {
    trgmChars(chars, start, len, acc0)
  }

  def trgmTokenMinEnd(token:String, acc0:List[String] = Nil) = {
    trgmCharsMinEnd(token.toCharArray, len=token.length, acc0=acc0)
  }


  /**
   * Триграммизация без доп.учета начала-конца токена.
   * По сути тут чистый набор триграмм из символов в указанном диапазоне.
   * @param chars буквы
   * @param start начало
   * @param len длина
   * @return список триграмм
   */
  @tailrec final def trgmCharsMin(chars:Array[Char], start:Int=0, len:Int, acc:List[String] = Nil) : List[String] = {
    if (len > 2) {
      val start1 = start+1
      val chArr = Array(chars(start), chars(start1), chars(start+2))
      val trgm  = new String(chArr)
      trgmCharsMin(chars, start1, len-1, trgm::acc)
    } else {
      acc
    }
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
  @tailrec private def trgmChars(chars:Array[Char], start:Int, len:Int, acc:List[String]) : List[String] = {
    if (len > 2) {
      // Выполняется обход слова, которое ещё не кончается.
      val start1 = start+1
      val chArr = Array(chars(start), chars(start1), chars(start+2))
      val trgm = new String(chArr)
      trgmChars(chars, start1, len-1, trgm::acc)
    } else if (len == 0) {
      // Пустое барахло - остановить обход.
      acc
    } else {
      val trgmChArr: Array[Char] = if (len == 2) {
        // Конец токена. Завершить рекурсию.
        Array(chars(start), chars(start+1), ' ')
      } else {
        // len == 1 -- Какой-то предлог внезапный.
        Array(' ', chars(start), ' ')
      }
      new String(trgmChArr) :: acc
    }
  }


  /**
   * Нормализация текста. Следует ещё nkfd делать.
   * @param text Юникодная строка для нормализации
   * @return Нормализованная строка.
   */
  def normalize(text: String) = text.toLowerCase


  /* Исправление неправильных символов (но внешне - правильных) в словах. */

  type MischarMapperPf_t = Char => Char

  /** Костыль для транляции скрытых символов визуальной транслитерации. Обычно ошибки в букве с,
    * которая русская и английская на на одной кнопке.
    * НЕЛЬЗЯ делать через PartialFunction, т.к. там постоянный box-unbox порождает огромные кучи мусора. */
  def mischarFixRu(ch: Char): Char = {
    ch match {
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
      case '7' => 'Т'
      case _   => ch
    }
  }


  /** Исправить русские буквы на схожие по начертанию английские. */
  def mischarFixEnAlpha(ch: Char): Char = {
    ch match {
      case 'с' => 'c'
      case 'С' => 'c'
      case 'Т' => 'T'
      case 'о' => 'o'
      case 'О' => 'O'
      case 'а' => 'a'
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
      case _   => ch
    }
  }

  /** Костыль для отката скрытой визуальной транслитерации на english.
   * НЕЛЬЗЯ делать через PartialFunction, т.к. там постоянный box-unbox порождает огромные кучи мусора. */
  def mischarFixEn(ch: Char): Char = {
    ch match {
      case '0' => 'O'
      case '1' => 'l'
      case '3' => 'E'
      case '7' => 'T'
      case other => mischarFixEnAlpha(other)
    }
  }


  private type AlpChPeriods_t = List[(Char, Char)]
  private type MischarChPeriodMap_t = List[(AlpChPeriods_t, MischarMapperPf_t)]

  /** Неизменяемая карта диапазонов алфавитов и repair-функций для этих алфавитов. Можно без private. */
  private val mischarChPeriodMap: MischarChPeriodMap_t = List(
    // TODO Периоды можно расширить через карты https://en.wikipedia.org/wiki/Plane_(Unicode)
    List('а' -> 'я', 'А' -> 'Я') -> mischarFixRu,
    List('a' -> 'z', 'A' -> 'Z') -> mischarFixEn
  )


  /**
   * Эта функция используется как враппер над mischarFixChArr(). Эта функция тут в основном для удобства тестирования.
   * В Lucene-фильтрах mischarFixChArr() используется напрямую, которая работает с массивами символов и без мусора.
   * @param word Исходная строка.
   * @return Новая строка, которая состоит из исправленных символов.
   */
  def mischarFixString(word: String): String = {
    val chArr = word.toCharArray
    mischarFixChArr(chArr, start=0, len=chArr.length)
    new String(chArr)
  }

  /**
   * Слово может иметь ошибочно (или намеренно) вставленные символы в иной раскладке, которые выглядят как натуральные.
   * Такое является особенно касается буквы c. Исправление строки, заданной в массиве символов с нулевым
   * порождением мусора. Исправление происходит прямо внутри переданного массива.
   * @param chArr Массив символов.
   * @param start Начало массива.
   * @param len Длина массива.
   */
  def mischarFixChArr(chArr: Array[Char], start:Int, len:Int) {
    mischarDetectRepairChArr(chArr, start=start, len=len, mischarChPeriodMap)
  }

  /**
   * Рекурсивная фунцкия для обнаружения и исправлея в строках ошибок, связанных с использованием неправильных, но
   * визуально корректных, символов.
   * @param chArr Исходный массив символов.
   * @param start Индекс начала строки.
   * @param len Кол-во символов для обработки.
   * @param mischarChPeriodRest Карта-список для обнаружения и исправления проблем.
   */
  @tailrec private def mischarDetectRepairChArr(chArr: Array[Char], start:Int, len:Int, mischarChPeriodRest: MischarChPeriodMap_t) {
    if (!mischarChPeriodRest.isEmpty) {
      val e = mischarChPeriodRest.head
      val cc = countCharsForChPeriod(chArr, start=start, len=len, alpChPeriods=e._1)
      val ccRel = cc.toFloat / len.toFloat
      val isMixChars = ccRel < 1.0F
      if (isMixChars && ccRel > WORD_MISCHAR_THRESHOLD) {
        // В слове есть смесь символов, но текущий язык доминирует. Чиним слово с помощью текущей функции исправления.
        repairChArrUsing(chArr, start=start, len=len, e._2)
      } else {
        // Текущий алфавит не доминирует в кривом слове. Поискать в другом алфавите.
        mischarDetectRepairChArr(chArr, start=start, len=len, mischarChPeriodRest.tail)
      }
    }
  }


  /** Подсчет кол-ва символов, относящихся к указанному диапазону без порождения мусора.
    * Исключением может являться сама tailrec-функция, которую компилятор нередко оборачивает в отдельный класс.
    * @param chArr Исходный массив символов.
    * @param start Стартовый (и текущий) индекс в массиве.
    * @param len Оставшееся кол-во символов до окончания обработки.
    * @param alpChPeriods Алфавит символов, заданный списком периодов.
    * @param sum Аккамулятор-счетчик результата.
    * @return Кол-во символов в исходном массиве, которые лежат в пределах указанного алфавита.
    */
  @tailrec private def countCharsForChPeriod(chArr:Array[Char], start:Int, len:Int, alpChPeriods:AlpChPeriods_t, sum:Int = 0): Int = {
    if (len > 0) {
      val ch = chArr(start)
      val sum1 = if (matchChAgainstPeriods(ch, alpChPeriods)) {
        sum + 1
      } else {
        sum
      }
      countCharsForChPeriod(chArr, start+1, len-1, alpChPeriods, sum1)
    } else {
      // Остаток длины равен нулю, значит обход массива закончен.
      sum
    }
  }

  /**
   * Проверить символ на соответствие списку диапазонов без порождения мусора.
   * @param ch Символ.
   * @param alpChPeriods Список диапазонов символов алфавита.
   * @return true - если символ удовлетворяет хотя бы одному диапазону. Иначе false.
   */
  @tailrec private def matchChAgainstPeriods(ch:Char, alpChPeriods: AlpChPeriods_t): Boolean = {
    if (alpChPeriods.isEmpty) {
      false
    } else {
      val (chStart, chEnd) = alpChPeriods.head
      if (ch >= chStart && ch <= chEnd) {
        true
      } else {
        matchChAgainstPeriods(ch, alpChPeriods.tail)
      }
    }
  }

  /**
   * Фунция "ремонтирует" строку, заданную в виде массива символов. Изменения происходят прямо в массиве.
   * @param chArr Исходный массив символов.
   * @param start Начало подстроки в массиве.
   * @param len Конец подстроки в массиве.
   * @param fixF Функция исправления ошибок.
   */
  @tailrec private def repairChArrUsing(chArr:Array[Char], start:Int, len:Int, fixF:MischarMapperPf_t) {
    if (len > 0) {
      val ch0 = chArr(start)
      val ch1 = fixF(ch0)
      chArr(start) = ch1
      repairChArrUsing(chArr, start + 1, len - 1, fixF)
    }
  }

}
