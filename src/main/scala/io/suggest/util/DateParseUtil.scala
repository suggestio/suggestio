package io.suggest.util

import util.matching.Regex
import com.github.nscala_time.time.Imports._
import java.util.Locale
import collection.mutable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.03.13 19:04
 * Description: Тут система извлечения дат из обычных текстов. Даты могут быть в самом различном формате.
 * Это порт модулей sio_text_dates.erl и sio_text_dates_month.erl на scala/java.
 */

object DateParseUtil extends Logs {

  // Заготовки для простого составления сложных регэкспов.
  // Пунктуация
  protected val RE_DMY_LOC_DELIM  = "[\\s\\p{P}/]*"   // Разделитель локализованной части от других
  protected val RE_WSPACE         = "\\s+"
  protected val RE_DMY_DELIM      = "[-./\\s\\\\]+"   // Разделитель цифровых частей
  // Элементы
  protected val RE_DAY            = "((3[01])|([12]\\d)|(0?[1-9]))"
  protected val RE_DAY2           = "((3[01])|([12]\\d)|(0[1-9]))"  // используется для DD-представления дня в таймштампах
  protected val RE_MONTH_I        = "((11)|(12)|(0?[1-9]))"
  protected val RE_MONTH_I2       = "((11)|(12)|(0[1-9]))"          // форсировать MM-представление месяца
  protected val RE_MONTH_L        = "(\\p{L}{3,16})"                // "декабрь", "дек.", "DeCemBER", etc
  protected val RE_YEAR4          = "(((19)|(20))\\d\\d)"           // пока доступны только YYYY-форматы.


  // Конструируются готовые регэкспы и replace-паттерны для них.
  // Replace нужен, чтобы получить дату в единый YMD-формат на выходе.
  val re_date_num_ymd_fusion = (("\\b" + RE_YEAR4 + RE_MONTH_I2 + RE_DAY2 + "\\b").r,              "$1 $5 $9")  // 20040201
  val re_date_num_dmy = ((RE_DAY + RE_DMY_DELIM + RE_MONTH_I + RE_DMY_DELIM + RE_YEAR4).r,         "$9 $5 $1")  // 22.02.2004
  val re_date_num_ymd = ((RE_YEAR4 + RE_DMY_DELIM + RE_MONTH_I + RE_DMY_DELIM + RE_DAY).r,         "$1 $5 $9")  // 2004/02/22. 2004-2-22
  // Даты с локализованными месяцами
  val re_date_loc_dmy = ((RE_DAY + RE_DMY_LOC_DELIM + RE_MONTH_L + RE_DMY_LOC_DELIM + RE_YEAR4).r, "$6 $5 $1") // 16 февраля 2004, 12/AUG/2004
  val re_date_loc_ymd = ((RE_YEAR4 + RE_DMY_LOC_DELIM + RE_MONTH_L + RE_DMY_LOC_DELIM + RE_DAY).r, "$1 $5 $6") // 2004 Feb 16, 2004/AUG/12
  val re_date_loc_ydm = ((RE_YEAR4 + RE_DMY_LOC_DELIM + RE_DAY + RE_WSPACE + RE_MONTH_L).r,        "$1 $9 $5") // 2004, 12 December.

  // Описываем функцию парсинга месяца в числовом и локализованном форматах. Типа интерфейс для функции.
  type ParseMonthF = String => Option[Short]

  /**
   * Сконвертить числовую строку с номером месяца в число. Если номер месяца не очень, то вернуть None.
   */
  val maybe_int_month_to_int : ParseMonthF = { month_str =>
    val maybe_month_num = month_str.toShort
    if (maybe_month_num > 0 && maybe_month_num <= 12)
      Some(maybe_month_num)
    else
      None
  }

  /**
   * Распарсить месяц, заданный в локализованном виде. Нужно обратиться к локалями и триграммам.
   */
  val maybe_loc_month_to_int : ParseMonthF = detectMonth(_:String)


  // Тут описывается программа работы системы. Первый элемент кортежа - функция матчинга
  protected val actions : List[(ParseMonthF, List[(Regex, String)])] = List(
    (maybe_int_month_to_int, List(re_date_num_ymd_fusion, re_date_num_dmy, re_date_num_ymd)),
    (maybe_loc_month_to_int, List(re_date_loc_dmy,        re_date_loc_ymd, re_date_loc_ydm))
  )


  /**
   * Функция извлечения даты из текста. Самая главная функция API этого модуля.
   * @param text Текст, которые вероятно содержит даты.
   * @return Список обнаруженных дат. В частности пустой список, если дат не найдено.
   */
  def extractDates(text:String) : List[LocalDate] = {
    // Пройти по списку action, потом по списку регэкспов-реплейсов экшена и обработать текст.
    actions.foldLeft(List[LocalDate]()) { case (acc0, (parseMonthF, patterns)) =>
      patterns.foldLeft(acc0) { (_acc0, _patternAndreplace) =>
        extractDatesWith(_patternAndreplace, text, parseMonthF, _acc0)
      }
    }
  }


  /**
   * Функция подхвата одного re_date_*-кортежа. Вынесена из extractDates для облегчения тестирования и просто
   * разделения логики. В sio_text_dates функция называлась "extract_ymd16", что не отражало её суть.
   * @param patternAndReplace кортеж из регэкспа и паттерна перестановки.
   * @param text Текст для обработки.
   * @param parseMonthF Функция парсинга месяца.
   */
  def extractDatesWith(patternAndReplace:(Regex, String), text:String, parseMonthF:ParseMonthF, acc0:List[LocalDate] = List()) : List[LocalDate] = {
    val (re, replace) = patternAndReplace
    re.findAllIn(text).foldLeft(acc0) { (_acc, _matched) =>
      re.replaceFirstIn(_matched, replace).split(' ') match {
        case _dtokens if _dtokens.length == 3 =>
          try {
            // Сначала попытаться распарсить месяц. Это снизит вероятность экзепшена внутри try, т.е. ускорит работу.
            parseMonthF(_dtokens(1)) match {
              // Есть месяц. Сгенерить дату и закинуть в аккамулятор.
              case Some(month_short) =>
                val ld = new LocalDate(_dtokens(0).toInt, month_short, _dtokens(2).toInt)
                ld :: _acc

              // Месяц не удалось определить. Пожаловаться в лог и забыть.
              case _ =>
                debug("Cannot parse month from date str: " + _matched)
                _acc
            }

          // Ошибка при парсинге даты. Вобщем-то, нередкая ситуация.
          } catch {
            case ex:Throwable =>
              error("Cannot parse date: " + _matched + " :: " + ex.getLocalizedMessage)
              _acc
          }

        // split не смог разделить строку на три части. Такое маловероятно, обычно означает ошибку в регэкспе или около.
        case _ =>
          error("Cannot split '" + _matched + "' into 3 parts.")
          _acc
      }
    }
  }


  /***********************************************************************************************
    *** Localized months routines
    **********************************************************************************************/

  // Не считать определение даты за валидное, если число совпадений триграмм ниже этой планки. 2 или 3.
  val MIN_TRGM_MONTH_RATING = 2

  // Список локалей, которые поддерживаются системой распознования дат
  // Можно завернуть внутрь генератора datesTrgmMap.
  val locales : Array[Locale] = Array(
    Locale.ENGLISH,
    new Locale("ru", "RU"),
    Locale.GERMAN
  )

  // Готовый словарь триграмм вида Map("янв" -> [1], "jan" -> [1], "арь" -> [1,2], ...)
  val datesTrgmMap = {
    locales.foldLeft(Map[String, Set[Short]]()) { (dictAcc, locale) =>
      val localeDict = monthNamesTrgmDict(locale)
      Lists.mergeMaps(localeDict, dictAcc) { (_, s1, s2) => s1 ++ s2 }
    }
  }


  /**
   * Сгенерить длинные и короткие имена месяца, нормализовать их и вернуть списком строк.
   * @param monthNum номер месяца: 1..12
   * @param locale локаль
   */
  def monthNamesLocalized(monthNum:Short, locale:Locale) : List[String] = {
    val dt = DateTime.now.withMonth(monthNum)
    Array("MMM", "MMMM").foldLeft(List[String]()) { (_acc, _fmt) =>
      val monthLoc = DateTimeFormat.forPattern(_fmt).withLocale(locale).print(dt)
      TextUtil.normalize(monthLoc) :: _acc
    }
  }


  /**
   * Создать словарь триграмм и числовых номеров месяцев для одной локали.
   * @param locale Locale, передаваемая в monthNamesLocalized
   * @return Map(trgm:String -> months:Set[Short])
   */
  def monthNamesTrgmDict(locale:Locale) : Map[String, Set[Short]] = {
    val monthTrgms = 1 to 12 map { _monthNumInt =>
      val _monthNum = _monthNumInt.toShort
      val trgms = monthNamesLocalized(_monthNum, locale)
        .flatMap {_monthName => TextUtil.trgm_token_full(_monthName) }
        .distinct
      (_monthNum, trgms)
    }
    Lists
      .insideOut(monthTrgms.toMap)
      .filter { case (trgmStr, months) => months.size <= 10 }
  }


  /**
   * frontend-функция для детектирования месяца. Получает слово на вход и возвращает месяц на выходе (если таков обнаружен)
   * @param word Строка со словом, которое, вероятно, отражает название месяца.
   * @return Номер месяца (1..12) если найден.
   */
  def detectMonth(word:String) : Option[Short] = {
    val wordTrgms = TextUtil.trgm_token_full(TextUtil.normalize(word))
    // Сгенерить словарь вида Map(1->3, 2->1, ... monthNum -> rating).
    // Используем mutable collection для упрощения алгоритма
    val mutMap = mutable.Map[Short, Int]()
    wordTrgms.foreach { trgm =>
      datesTrgmMap.get(trgm) match {
        // Есть подходящая триграмма - инкрементировать её рейтинг
        case Some(setOfMonths) =>
          setOfMonths.foreach { _monthNum =>
            val currRating = mutMap.getOrElse(_monthNum, 0)
            mutMap(_monthNum) = currRating + 1
          }

        case None =>
      }
    }
    // Найти наиболее подходящий по рейтингу месяц.
    val maxTuple = mutMap.foldLeft((0.toShort, 0)) { (_acc, _mv) =>
      val _currValue = _mv._2
      if (_currValue >= MIN_TRGM_MONTH_RATING && _currValue >= _acc._2)
        _mv
      else
        _acc
    }
    maxTuple._1 match {
      case 0 => None
      case n => Some(n)
    }
  }

}
