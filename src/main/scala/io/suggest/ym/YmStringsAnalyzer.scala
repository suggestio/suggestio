package io.suggest.ym

import org.apache.lucene.util.Version.{LUCENE_4_9 => luceneVsn}
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.standard._
import org.apache.lucene.analysis.core._
import org.tartarus.snowball.ext.{RussianStemmer, EnglishStemmer}
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import org.apache.lucene.analysis.snowball.SnowballFilter
import org.apache.lucene.analysis.{Analyzer, TokenStream}
import java.io.Reader
import org.apache.lucene.analysis.util.CharArraySet
import java.util
import scala.collection.JavaConversions._
import scala.util.parsing.combinator.JavaTokenParsers
import parsers._
import io.suggest.an.ReplaceMischarsAnalyzer
import io.suggest.util.MyConfig.CONFIG

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.14 14:54
 * Description: Поля param имеют локализованное названия в довольно свободной форме.
 * Нужно применить знания языка и нормализовать эти строковые выхлопы, а затем распарсить.
 * Для расширения фунционала используется пакет parse, содержимое которого подмешивается в тутошние анализаторы.
 */
object YmStringsAnalyzer extends JavaTokenParsers {

  /** Фильтровать слова из названия категорий, если длина слова длинее n символов. */
  val MAX_TOKEN_LEN = CONFIG.getInt("ym.string.an.token.len.max") getOrElse 40

  /** Дефолтовые русские и английские стоп-слова содержат лишние слова. Тут отфильтровываем их из исходного списка. */
  private def filterStops(cas: CharArraySet, nonStop:List[String]) = {
    val nonStopCh = nonStop.map(_.toCharArray)
    val stopSetFilterF: PartialFunction[AnyRef, Boolean] = {
      case dfltStopword: Array[Char] =>
        !nonStopCh.exists {
          chsNS  =>  util.Arrays.equals(dfltStopword, chsNS)
        }
    }
    cas.iterator()
      .filter(stopSetFilterF)
      .asInstanceOf[Iterator[Array[Char]]]
  }

  /** Неизменяемое потоко-безопасное множество стоп-слов. Используется при сборке анализатора текстов. */
  val PARAM_TEXT_STOPWORD_SET = {
    val ruNonStopCh = List(
      "все", "только", "когда", "человек", "раз", "жизнь", "два", "другой", "больше", "всего", "всегда"
    )
    val ruStop = filterStops(RussianAnalyzer.getDefaultStopSet, ruNonStopCh)
    val enNonStopCh = List("during", "all")
    // TODO Может использовать english_stop.txt? EnglishAnalyzer использует старые захардкоженные стопы.
    val enStop = filterStops(EnglishAnalyzer.getDefaultStopSet, enNonStopCh)
    val mergedStopCh = ruStop ++ enStop
    val mergedStop = mergedStopCh map { new String(_) }
    CharArraySet.unmodifiableSet(
      StopFilter.makeStopSet(luceneVsn, mergedStop.toArray, false)
    )
  }


  def token2string(stream: TokenStream): String = {
    stream.getAttribute(classOf[CharTermAttribute]).toString
  }
}


import YmStringsAnalyzer._

/** thread-local class для маломусорной lucene-нормализации коротких строчек. */
class YmStringsAnalyzer
extends Analyzer(Analyzer.GLOBAL_REUSE_STRATEGY)
// статически-вкомпиленные плагины добавляются тут:
with TextNormalizerAn
with NormTokensOutAn
with ParamNameParserAn
with MassUnitParserAn {

  /** Сборка анализатора текстов происходит здесь. */
  def createComponents(fieldName: String, reader: Reader): TokenStreamComponents = {
    // Заменить букву ё на е.
    val tokens = new StandardTokenizer(luceneVsn, reader)
    tokens.setMaxTokenLength(MAX_TOKEN_LEN)
    var filtered: TokenStream = new StandardFilter(luceneVsn, tokens)  // https://stackoverflow.com/a/16965481
    filtered = new ReplaceMischarsAnalyzer(luceneVsn, filtered)
    filtered = new LowerCaseFilter(luceneVsn, filtered)
    // Выкинуть стоп-слова.
    filtered = new StopFilter(luceneVsn, filtered, PARAM_TEXT_STOPWORD_SET)
    // Стеммеры для отбрасывания окончаний.
    filtered = new SnowballFilter(filtered, new RussianStemmer)
    filtered = new SnowballFilter(filtered, new EnglishStemmer)
    new TokenStreamComponents(tokens, filtered)
  }
}


trait NormTokensOutAn extends Analyzer {
 
  /** Собрать набор токенов из строки в виде списка. Список в обратном порядке. */
  def toNormTokensRev(src: String): List[String] = {
    var acc: List[String] = Nil
    val stream = tokenStream(null, src)
    try {
      stream.reset()
      while (stream.incrementToken()) {
        val resultToken = token2string(stream)
        acc ::= resultToken
      }
    } finally {
      stream.close()
    }
    acc
  }

  /** Тоже самое, что и toNormTokensRev(String), но токены в прямом порядке. */
  def toNormTokensDirect(src: String) = toNormTokensRev(src).reverse
}


trait TextNormalizerAn extends Analyzer {
  /** Нормализовать имя параметра, заданного в произвольной форме. */
  def normalizeText(pn: String): StringBuilder = {
    val readyTokens = new StringBuilder
    val stream = tokenStream(null, pn)
    try {
      stream.reset()
      while (stream.incrementToken()) {
        val resultToken = token2string(stream)
        readyTokens append resultToken
      }
    } finally {
      stream.close()
    }
    readyTokens
  }
}

