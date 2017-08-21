package io.suggest.text.an

import java.io.Reader
import java.util

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.apache.lucene.analysis.snowball.SnowballFilter
import org.apache.lucene.analysis.standard._
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.analysis._
import org.tartarus.snowball.ext.{EnglishStemmer, RussianStemmer}

import scala.collection.JavaConverters._
import scala.util.parsing.combinator.JavaTokenParsers

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
  def MAX_TOKEN_LEN = 40

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
      .asScala
      .filter(stopSetFilterF)
      .asInstanceOf[Iterator[Array[Char]]]
  }

  /** Неизменяемое потоко-безопасное множество стоп-слов. Используется при сборке анализатора текстов. */
  lazy val PARAM_TEXT_STOPWORD_SET = {
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
      StopFilter.makeStopSet(mergedStop.toArray, false)
    )
  }

  def token2string(stream: TokenStream): String = {
    stream.getAttribute(classOf[CharTermAttribute]).toString
  }

}


import io.suggest.text.an.YmStringsAnalyzer._


/** Трейт с переопределяемыми методами сборки привычного YmString-анализатора. */
trait YmStringAnalyzerT extends Analyzer {

  def tokenizer(): Tokenizer = {
    val tok = new StandardTokenizer()
    tok.setMaxTokenLength(MAX_TOKEN_LEN)
    tok
  }

  def addFilters(tokenized: Tokenizer): TokenStream = {
    // Заменить букву ё на е.
    var filtered: TokenStream = new StandardFilter(tokenized)  // https://stackoverflow.com/a/16965481
    filtered = new ReplaceMischarsAnalyzer(filtered)
    filtered = new LowerCaseFilter(filtered)
    // Выкинуть стоп-слова.
    filtered = new StopFilter(filtered, PARAM_TEXT_STOPWORD_SET)
    // Стеммеры для отбрасывания окончаний.
    filtered = new SnowballFilter(filtered, new RussianStemmer)
    filtered = new SnowballFilter(filtered, new EnglishStemmer)
    filtered
  }

  /** Сборка анализатора текстов происходит здесь. */
  override def createComponents(fieldName: String): TokenStreamComponents = {
    val tokens = tokenizer()
    val filtered = addFilters(tokens)
    new TokenStreamComponents(tokens, filtered)
  }

}


trait AnalyzerUtil extends Analyzer {

  /** Абстрактный прогонятель исходных данных через анализатор. Каждый токен попадает в handleResultToken. */
  trait TokenStreamUnfold {
    def getStream: TokenStream
    def handleResultToken(token: String): Unit
    def unfoldStream(): this.type = {
      val _stream = getStream
      try {
        _stream.reset()
        while (_stream.incrementToken()) {
          val resultToken = token2string(_stream)
          handleResultToken(resultToken)
        }
      } finally {
        _stream.close()
      }
      this
    }
  }

  /** Прогнать строку через анализатор. */
  trait StringTokenStreamUnfold extends TokenStreamUnfold {
    def _src: String
    override def getStream = tokenStream(null, _src)
  }

  /** Прогнать Reader через аналиатор. */
  trait ReaderTokenStreamUnfold extends TokenStreamUnfold {
    def _src: Reader
    override def getStream = tokenStream(null, _src)
  }

  // TODO Нужен iterator-интерфейс для обхода TokenStream.
}

trait NormTokensOutAn extends AnalyzerUtil {

  /** Собрать набор токенов из строки в виде списка. Список в обратном порядке. */
  def toNormTokensRev(src: String, acc0: List[String] = Nil): List[String] = {
    val unfolder = new StringTokenStreamUnfold {
      var _acc: List[String] = acc0
      override def _src = src
      override def handleResultToken(token: String): Unit = {
        _acc ::= token
      }
    }
    unfolder.unfoldStream()._acc
  }

  /** Тоже самое, что и toNormTokensRev(String), но токены в прямом порядке. */
  def toNormTokensDirect(src: String) = toNormTokensRev(src).reverse
}

/** Аналог [[NormTokensOutAn]], но работает с reader-ом, а не со строкой. */
trait NormTokensOutAnStream extends AnalyzerUtil {
  def normTokensReaderRev(reader: Reader, acc0: List[String] = Nil): List[String] = {
    val unfolder = new ReaderTokenStreamUnfold {
      var _acc: List[String] = acc0
      override def _src = reader
      override def handleResultToken(token: String): Unit = {
        _acc ::= token
      }
    }
    unfolder.unfoldStream()._acc
  }


  def normTokensReaderDirect(reader: Reader): List[String] = {
    normTokensReaderRev(reader).reverse
  }
}


trait TextNormalizerAn extends AnalyzerUtil {
  /** Нормализовать имя параметра, заданного в произвольной форме. */
  def normalizeText(pn: String): StringBuilder = {
    val sb = new StringBuilder
    val unfolder = new StringTokenStreamUnfold {
      override def _src: String = pn
      override def handleResultToken(token: String): Unit = {
        sb append token
      }
    }
    unfolder.unfoldStream()
    sb
  }
}

