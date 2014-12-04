package util.ai.sax.weather.gidromet

import java.io.{CharArrayReader, Reader}

import io.suggest.an.ReplaceMischarsAnalyzer
import io.suggest.util.DateParseUtil
import io.suggest.ym.{NormTokensOutAnStream, YmStringAnalyzerT}
import models.ai.AiParsers.AiParser
import models.ai._
import org.apache.lucene.analysis.core.LowerCaseFilter
import org.apache.lucene.analysis.snowball.SnowballFilter
import org.apache.lucene.analysis.standard.StandardFilter
import org.apache.lucene.analysis.{TokenStream, Tokenizer}
import org.apache.lucene.analysis.pattern.PatternTokenizer
import org.joda.time.{LocalDate, DateTime}
import org.tartarus.snowball.ext.RussianStemmer
import org.xml.sax.{SAXParseException, Attributes}
import org.xml.sax.helpers.DefaultHandler
import util.PlayLazyMacroLogsImpl
import util.ai.AiContentHandler
import util.ai.sax.StackFsmSax

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.11.14 13:01
 * Description: Парсер данных от гидромета по RSS с помощью SAX.
 * Пример RSS: http://meteoinfo.ru/rss/forecasts/26063
 * 2014.12.04: Поддержка tika выкинута.
 */
class GidrometRssSax(maim: MAiCtx)
  extends DefaultHandler
  with StackFsmSax
  with AiContentHandler
  with PlayLazyMacroLogsImpl
{

  import LOGGER._

  override def stiResKey = "weather"

  /** Этот парсер в основном работает с tika, хотя наверное лучше его на SAX переключить. */
  override def sourceParser: AiParser = AiParsers.SaxTolerant

  /** Для нормализации строк с погодой используется сие добро: */
  protected val an = new YmStringAnalyzerT with NormTokensOutAnStream {
    override def tokenizer(reader: Reader): Tokenizer = {
      val re = "[\\s,.]+".r.pattern
      new PatternTokenizer(reader, re, -1)
    }

    override def addFilters(tokenized: Tokenizer): TokenStream = {
      var filtered: TokenStream = new StandardFilter(_luceneVsn, tokenized)  // https://stackoverflow.com/a/16965481
      filtered = new ReplaceMischarsAnalyzer(_luceneVsn, filtered)
      filtered = new LowerCaseFilter(_luceneVsn, filtered)
      filtered = new SnowballFilter(filtered, new RussianStemmer)
      filtered
    }
  }

  /** Аккамулятор результатов в обратном порядке. */
  var accRev: List[DayWeatherBean] = Nil

  /** Дата-время во время запуска этой канители. */
  val now = DateTime.now
  val today = now.toLocalDate

  val DAY_OF_MONTH_RE = DateParseUtil.RE_DAY.r

  /** Набор парсеров в рамках этого инстанса, который исполняется лишь в одном потоке. */
  val wParsers = new GidrometParsersVal {}

  /** Начинается документ. Пора выставить начальный обработчик. */
  override def startDocument(): Unit = {
    become(new TopLevelHandler)
    super.startDocument()
  }

  /** Обработчик корня стека. */
  class TopLevelHandler extends TopLevelHandlerT {
    override def startTag(tagName: String, attributes: Attributes): Unit = {
      val nextState = if (tagName == "rss") {
        new RssTagHandler(attributes)
      } else {
        unexpectedTag(tagName)
      }
      become(nextState)
    }
  }


  /** Тег содержащий данные об одном элементе. */
  trait ItemElementHandlerT extends TagHandler {
    val dw = DayWeatherAcc(today)

    /** Тег, содержащий title */
    trait TitleElementHandlerT extends TagHandler {
      val sb = new StringBuilder(64)

      override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
        sb.appendAll(ch, start, length)
      }

      override def onTagEnd(tagName: String): Unit = {
        super.onTagEnd(tagName)
        // Нужно отковырять из доки ту дату, которая имелась в виду. Гидрометцентр не указывает год, только день и месяц.
        val reader = new CharArrayReader(sb.toArray)
        val tokensRev = today.getYear.toString :: an.normTokensReaderRev(reader)
        val s = tokensRev.reverse.mkString(" ")
        DateParseUtil
          .extractDates(s)
          .headOption
          .foreach { d0 =>
            dw.date = d0
          }
      }
    }

    trait DescrSubElementHandlerT extends DescrElementHandlerT {
      override def _dw = dw
    }
  }

  trait DescrElementHandlerT extends TagHandler {
    val sb = new StringBuilder(512)
    def _dw: DayWeatherAcc

    override def thisTagName = "description"
    override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
      sb.appendAll(ch, start, length)
    }
    override def onTagEnd(tagName: String): Unit = {
      super.onTagEnd(tagName)
      val reader = new CharArrayReader(sb.toArray)
      val tokens = an.normTokensReaderDirect(reader)
        .mkString(" ")
      // Нужно пропарсить нормализованные токены с помощью парсеров.
      val p = wParsers.dayWeatherP(_dw)
      val pr = wParsers.parse(p, tokens)
      if (pr.successful) {
        accRev ::= pr.get
      } else {
        LOGGER.error("Unable to parse weather description string:\n " + tokens + "\n " + pr)
      }
    }
  }


  /** Обработчик тега верхнего уровня. */
  class RssTagHandler(val thisTagAttrs: Attributes) extends TagHandler {
    override def thisTagName = "rss"
    override def startTag(tagName: String, attributes: Attributes): Unit = {
      if (tagName == "channel") {
        become(new ChannelTagHandler(attributes))
      } else {
        unexpectedTag(tagName)
      }
    }
  }


  /** Обработчик channel-тега, который в гидромете является одиночным тегом верхнего уровня. */
  class ChannelTagHandler(val thisTagAttrs: Attributes) extends TagHandler {
    override def thisTagName = "channel"

    override def startTag(tagName: String, attributes: Attributes): Unit = {
      //val ignoredTagNames = Set("title", "name", "link", "description", "ttl")
      val nextState = if (tagName == "item") {
        new ItemTagHandler(attributes)
      } else {
        new DummyHandler(tagName, attributes)
      }
      become(nextState)
    }
  }


  /**
   * Обработчик item-тега, который содержит данные по одному элементу этого rss-канала.
   * В этом теге содержаться все полезные данные:
   * - Подтег title содержит дату.
   * - Подтег description содержит погоду.
   */
  class ItemTagHandler(val thisTagAttrs: Attributes) extends ItemElementHandlerT {
    override def thisTagName = "item"
    var sourceDate: Option[LocalDate] = None

    override def startTag(tagName: String, attributes: Attributes): Unit = {
      val nextState = if (tagName == "title") {
        new TitleTagHandler(attributes)
      } else if (tagName == "description") {
        new DescriptionTagHandler(attributes)
      } else if (tagName == "source") {
        new SourceTagHandler(attributes)
      } else {
        new DummyHandler(tagName, attributes)
      }
      become(nextState)
    }

    override def endTag(tagName: String): Unit = {
      if (sourceDate.isDefined)
        // TODO Прогноз, составленный 31 декабря будет выдавать неправильный год для первого января.
        dw.date = dw.date.withYear(sourceDate.get.getYear)
      super.endTag(tagName)
    }

    /** Тело title-тега содержит название города и дату, на которую приходится прогноз (без года!) */
    class TitleTagHandler(val thisTagAttrs: Attributes) extends TitleElementHandlerT {
      override def thisTagName = "title"
    }

    /** Парсер описания погоды. Здесь содержится вся инфа по погоде. */
    class DescriptionTagHandler(val thisTagAttrs: Attributes) extends DescrSubElementHandlerT {
      override def thisTagName = "description"
    }

    /** source содержит какую-то инфу об источнике данных. Отсюда нам нужен только год. */
    class SourceTagHandler(val thisTagAttrs: Attributes) extends TagHandler {
      override def thisTagName = "source"
      val sb = new StringBuilder(64)

      override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
        super.characters(ch, start, length)
        sb.appendAll(ch, start, length)
      }

      override def endTag(tagName: String): Unit = {
        DateParseUtil.extractDates(sb.toString())
          .headOption
          .foreach { d0 =>
            sourceDate = Some(d0)
          }
        super.endTag(tagName)
      }
    }
  }


  /**
   * Вернуть накопленный результат парсинга.
   * Если результата нет или он заведомо неверный/бесполезный, то должен быть экзепшен с причиной.
   * @return Реализация модели ContentHandlerResult.
   */
  override def getParseResult: WeatherForecastT = {
    new WeatherForecastT {
      override def getLocalTime: DateTime = {
        super.getLocalTime
          .withZone(maim.tz)
      }

      override val getToday: DayWeatherBean = accRev.find(_.date == today).get

      def findWithDays(d: Int): Option[DayWeatherBean] = {
        val tmr = today.plusDays(d)
        accRev.find(_.date == tmr)
      }
      override lazy val getTomorrow = findWithDays(1)
      override lazy val getAfterTomorrow = findWithDays(2)

      /** В зависимости от текущего времени нужно сгенерить прогноз погоды на день и ночь или наоборот. */
      override val getH24 = super.getH24
    }
  }

}

