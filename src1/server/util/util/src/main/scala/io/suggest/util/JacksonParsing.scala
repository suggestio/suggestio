package io.suggest.util

import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.time.{Instant, OffsetDateTime, ZoneOffset, ZonedDateTime}
import java.{lang => jl, util => ju}

import play.api.libs.json.{JsString, JsValue}

import scala.collection.JavaConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.02.17 15:05
  * Description: Устаревший и древний хлам, вынесенный из EsModelUtil.
  * За него цепляются всякие старые модели, всё ещё не портированные на play-json или n2.
  */
object JacksonParsing {

  /** Тип аккамулятора, который используется во EsModelPlayJsonT.writeJsonFields(). */
  type FieldsJsonAcc = List[(String, JsValue)]

  private def _parseEx(as: String, v: Any = null) = {
    throw new IllegalArgumentException(s"unable to parse '$v' as $as.")
  }

  // TODO Это устаревший код. Его нужно удалять, MEvent и MExtTarget тянут за собой эти старинные парсеры.
  // ES-выхлопы страдают динамической типизацией, поэтому нужна коллекция парсеров для примитивных типов.
  // Следует помнить, что любое поле может быть списком значений.

  def stringParser: PartialFunction[Any, String] = {
    case null =>
      _parseEx("string")
    case strings: jl.Iterable[_] =>
      stringParser(strings.asScala.head.asInstanceOf[AnyRef])
    case s: String  => s
  }
  def booleanParser: PartialFunction[Any, Boolean] = {
    case null =>
      _parseEx("bool")
    case bs: jl.Iterable[_] =>
      booleanParser(bs.asScala.head.asInstanceOf[AnyRef])
    case b: jl.Boolean =>
      b.booleanValue()
  }
  def dateTimeParser: PartialFunction[Any, OffsetDateTime] = {
    case null => null
    case dates: jl.Iterable[_] =>
      dateTimeParser(dates.asScala.head.asInstanceOf[AnyRef])
    case s: String           => OffsetDateTime.parse(s)
    case d: ju.Date          => dateTimeParser( d.toInstant )
    case dt: OffsetDateTime  => dt
    case zdt: ZonedDateTime  => zdt.toOffsetDateTime
    case ri: Instant         => ri.atOffset( ZoneOffset.UTC )
  }
  // Сериализация дат
  private def dateFormatterDflt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  def date2str(dateTime: TemporalAccessor): String = dateFormatterDflt.format(dateTime)
  def date2JsStr(dateTime: TemporalAccessor): JsString = JsString( date2str(dateTime) )


  /** Парсер json-массивов. */
  def iteratorParser: PartialFunction[Any, Iterator[Any]] = {
    case null =>
      Iterator.empty
    case l: jl.Iterable[_] =>
      l.iterator().asScala
  }

  /** Парсер список строк. */
  def strListParser: PartialFunction[Any, List[String]] = {
    iteratorParser andThen { iter =>
      iter.foldLeft( List.empty[String] ) {
        (acc,e) => stringParser(e) :: acc
      }.reverse
    }
  }

}
