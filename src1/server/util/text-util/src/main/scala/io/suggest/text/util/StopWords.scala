package io.suggest.text.util

import java.io.{BufferedReader, Reader}
import java.nio.charset.StandardCharsets

import org.apache.lucene.util.IOUtils

import scala.collection.mutable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.13 10:30
 * Description: Работа со списками стоп-слов из lucene.jar
 */
object StopWords {

  // Стоп-слова ищутся в org.apache.lucene.analysis.XX.stopwords.txt и org.apache.lucene.analysis.snowball.XXXX..._stop.txt
  val LANGS = List("russian", "english")

  val FILE_PREFIX = "org/apache/lucene/analysis/"

  // Все стоп-слова в одном неизменяемом множестве.
  val ALL_STOPS = {
    val acc0 = getStops(LANGS.head)
    LANGS.tail.foldLeft(acc0) { _ ++ getStops(_) }
  }

  /**
   * Генерация множества стоп-слов для указанного языка.
   * @param lang id языка. Для snowball - "russian", "english". Для не-snowball - "ca", "fa", etc
   * @return immutable set.
   */
  protected def getStops(lang: String) : Set[String] = {
    // В зависимости от длины id языка используем тот или иной тип парсера stopwords.txt.
    val (fileSuffix : String, swParser:StopWordsParser) = if (lang.length == 2)
      (lang + "/stopwords.txt", new SimpleStopWordsParser)
    else if (lang.length > 4)
      ("snowball/" + lang + "_stop.txt", new SnowballStopWordsParser)
    else
      throw new UnsupportedOperationException("Unsupported language: " + lang)

    val filePath = FILE_PREFIX + fileSuffix
    val stream = getClass.getClassLoader.getResourceAsStream(filePath)
    val reader = IOUtils.getDecodingReader(stream, StandardCharsets.UTF_8)
    // Загрузить стоп-слова в множество-аккамулятор
    val stops = mutable.HashSet[String]()
    swParser.loadStopwords(reader, stops)
    stops.toSet
  }

}


trait StopWordsParser {
  type AccT = mutable.Set[String]

  protected def loadStopwordsBR(reader:BufferedReader, acc:AccT): Unit

  def loadStopwords(reader:Reader, acc:AccT): Unit = {
    var br : BufferedReader = null
    try {
      br = new BufferedReader(reader)
      loadStopwordsBR(br, acc)
    } finally {
      IOUtils.close(br)
    }
  }
}

class SnowballStopWordsParser extends StopWordsParser {

  /**
   * Загрузить слова из формата snowball.
   * Калька с WordlistLoader.getSnowballWordSet().
   * @param reader читатель файла.
   * @param acc аккамулятор
   * @return
   */
  protected def loadStopwordsBR(reader: BufferedReader, acc: AccT): Unit = {
    var line:String = null
    // Проход по файлу в java-стиле.
    while({line = reader.readLine(); line != null}) {
      val comment = line.indexOf('|')
      if (comment >= 0)
        line = line.substring(0, comment)
      val words = line.split("\\s+")
      words.foreach{ e:String =>
        if (e.length > 0)
          acc.add(e)
      }
    }
  }

}


class SimpleStopWordsParser extends StopWordsParser {

  /**
   * Прочитать список стоп-слов в обычном формате. Одна строка - одно слово с комментами, начинающимися на #.
   * Калька с WordlistLoader.getWordSet()
   * @param reader Читатель файла.
   * @param acc аккамулятор
   * @return
   */
  protected def loadStopwordsBR(reader: BufferedReader, acc: AccT): Unit = {
    var line:String = null
    while({line = reader.readLine(); line != null}) {
      acc.add(line.trim)
    }
  }

}