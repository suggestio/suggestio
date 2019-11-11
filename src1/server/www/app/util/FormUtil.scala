package util

import java.net.{MalformedURLException, URL}
import java.time.{LocalDate, ZoneId}

import io.suggest.adn.edit.NodeEditConstants
import io.suggest.bill.{MCurrencies, MCurrency}
import io.suggest.common.empty.EmptyUtil
import io.suggest.dt.DateTimeUtil
import io.suggest.es.model.MEsUuId
import io.suggest.geo._
import io.suggest.model.n2.edge.{MPredicate, MPredicates}
import io.suggest.text.parse.dt.DateParseUtil
import io.suggest.text.util.UrlUtil
import models.blk.SzMult_t
import models.madn.{AdnShownType, AdnShownTypes}
import org.apache.commons.text.StringEscapeUtils
import org.elasticsearch.common.unit.DistanceUnit
import play.api.data.Forms._
import play.api.data.Mapping
import play.api.i18n.Lang
import util.tpl.HtmlSanitizer

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.13 19:01
 * Description: набор хелперов для форм.
 */
object FormUtil {

  def strIdentityF = {s:String => s}
  def strTrimF = {s: String => s.trim }
  def stripHtml(s: String) = HtmlSanitizer.stripAllPolicy.sanitize(s)
  def strTrimSanitizeF = { s:String =>
    // TODO Исключить двойные пробелы
    stripHtml(s).trim
  }

  def strTrimSanitizeLowerF = strTrimSanitizeF andThen { _.toLowerCase }
  def strFmtTrimF = {s: String =>
    // TODO прогонять через HtmlCompressor
    HtmlSanitizer.textFmtPolicy.sanitize(s).trim
  }

  def strUnescapeF = {s: String => StringEscapeUtils.unescapeHtml4(s) }

  /** sanitizer вызывает html escape для амперсандов, ковычек и т.д. Если это не нужно, то следует вызывать unescape ещё. */
  def strTrimSanitizeUnescapeF = strTrimSanitizeF andThen strUnescapeF


  /** Функция, которая превращает Some("") в None. */
  def emptyStrOptToNone(s: Option[String]) = {
    s.filter(!_.isEmpty)
  }

  def isValidUrl(urlStr: String): Boolean = {
    try {
      !urlStr.isEmpty && {
        new URL(urlStr)
        true
      }
    } catch {
      case _: Exception => false
    }
  }

  /** Маппер поля формы для опциональной time-зоны. */
  def timeZoneOptM: Mapping[Option[ZoneId]] = {
    val ntm = nonEmptyText(minLength = 2, maxLength = 64)
    toStrOptM(ntm, strTrimSanitizeF)
      .transform [Option[ZoneId]] (
        {tzRawOpt =>
          tzRawOpt.flatMap { tzRaw =>
            try {
              Option( ZoneId.of(tzRaw) )
            } catch {
              case ex: Exception => None
            }
          }
        },
        { _.map(_.getId) }
      )
  }
  /** Маппер поля формы для обязательной time-зоны. */
  def timeZoneM: Mapping[ZoneId] = {
    timeZoneOptM
      .verifying("error.invalid", { _.isDefined })
      .transform [ZoneId] ( EmptyUtil.getF, EmptyUtil.someF )
  }


  /** Конструктор для id-маппингов. */
  private def _esIdM(baseM: Mapping[String]): Mapping[String] = {
    baseM
      .transform(strTrimSanitizeF, strIdentityF)
      .verifying("error.invalid.id", MEsUuId.isEsIdValid(_))
  }

  private def ID_LEN_MIN = 19
  private def ID_LEN_MAX_UUID = 30

  /** id'шники в ES-моделях генерятся силами ES. Тут маппер для полей, содержащих ES-id. */
  def esIdM = _esIdM( nonEmptyText(minLength=ID_LEN_MIN, maxLength=ID_LEN_MAX_UUID) )


  /** Парсим текст, введённый в поле с паролем. */
  def passwordM = nonEmptyText
    .verifying("password.too.short", {_.length > 5})
    .verifying("password.too.long", {_.length <= 1024})

  /** Два поля: пароль и подтверждение пароля. Используется при регистрации пользователя. */
  def passwordWithConfirmM = tuple(
    "pw1" -> passwordM,
    "pw2" -> passwordM
  )
  .verifying("passwords.do.not.match", { pws => pws match {
    case (pw1, pw2) => pw1 == pw2
  }})
  .transform[String](
    { case (pw1, pw2) => pw1 },
    { _: AnyRef =>
      // Назад пароли тут не возвращаем никогда. Форма простая, и ошибка может возникнуть лишь при вводе паролей.
      val pw = ""
      (pw, pw)
    }
  )


  def adnShownTypeOptM: Mapping[Option[AdnShownType]] = {
    optional(
      nonEmptyText(maxLength = 10)
    )
      .transform[Option[AdnShownType]](
        _.flatMap( AdnShownTypes.withValueOpt ),
        _.map(_.toString)
      )
  }
  def adnShownTypeIdOptM: Mapping[Option[String]] = {
    adnShownTypeOptM
      .transform[Option[String]](_.map(_.toString), _.flatMap(AdnShownTypes.withValueOpt))
  }

  /** Единицы расстояния. */
  def distanceUnitsM: Mapping[DistanceUnit] = {
    nonEmptyText(minLength = 1, maxLength = 16)
      .transform[DistanceUnit] (DistanceUnit.fromString, _.toString)
  }

  /** Дистанция. */
  def distanceM: Mapping[Distance] = {
    mapping(
      "value" -> doubleM,
      "units" -> distanceUnitsM
    )
    { Distance.apply }
    { Distance.unapply }
  }


  /** Маппинг для географического круга, задаваемого географическим центром и радиусом. */
  def circleM: Mapping[CircleGs] = {
    mapping(
      "center"  -> geoPointM,
      "radius"  -> distanceM
        .transform[Double](_.meters, Distance.meters)
    )
    { CircleGs.apply }
    { CircleGs.unapply }
  }

  def strOptGetOrElseEmpty(x: Option[String]) = x getOrElse ""
  def toStrOptM(x: Mapping[String], trimmer: String => String = strTrimSanitizeUnescapeF): Mapping[Option[String]] = {
    x.transform[String](trimmer, strIdentityF)
     .transform[Option[String]](
       {s => Option(s).filter(!_.isEmpty)},
       strOptGetOrElseEmpty
     )
  }


  def nameM: Mapping[String] = {
    val n = NodeEditConstants.Name
    val minLen = n.LEN_MIN
    val maxLen = n.LEN_MAX
    nonEmptyText(minLength = minLen, maxLength = maxLen)
      .transform(strTrimSanitizeUnescapeF, strIdentityF)
      .verifying("error.too.short", _.length >= minLen)
  }


  def publishedTextM = text(maxLength = 2048)
    .transform(strFmtTrimF, strIdentityF)
  def publishedTextOptM = optional(publishedTextM)
    .transform [Option[String]] (emptyStrOptToNone, identity)

  def townM = nonEmptyText(maxLength = 32)
    .transform(strTrimSanitizeUnescapeF, strIdentityF)
  def townOptM = optional(townM)
    .transform [Option[String]] (emptyStrOptToNone, identity)

  def addressM = nonEmptyText(minLength = 6, maxLength = 128)
    .transform(strTrimSanitizeUnescapeF, strIdentityF)
  def addressOptM = optional(addressM)
    .transform [Option[String]] (emptyStrOptToNone, identity)


  // TODO Нужен нормальный валидатор телефонов.
  def phoneM = nonEmptyText(minLength = 5, maxLength = 50)
    .transform(strTrimSanitizeUnescapeF, strIdentityF)
  def phoneOptM = optional(phoneM)
    .transform [Option[String]] (emptyStrOptToNone, identity)
  //def phoneSomeM = toSomeStrM(phoneM)


  def text2048M: Mapping[String] = {
    text(maxLength = 2048)
      .transform(strTrimSanitizeF, strIdentityF)
  }


  /** Маппер form-поля URL в строку URL */
  def urlNormalizeSafe(s: String): String = {
    try {
      UrlUtil.normalize(s)
    } catch {
      case _: Exception => ""
    }
  }
  def urlStrM = nonEmptyText(minLength = 8)
    .transform[String](strTrimF andThen urlNormalizeSafe, UrlUtil.humanizeUrl)
    .verifying("mappers.url.invalid_url", isValidUrl(_))
  def urlStrOptM = optional(urlStrM)

  /** Маппер опционального form-поля с ссылкой в java.net.URL. */
  def urlOptM: Mapping[Option[URL]] = {
    val m1 = nonEmptyText(minLength = 8, maxLength = 1024)
    // TODO Нужно различать случаи, когда None из-за неправильной ссылки и None из-за пустой строки.
    toStrOptM(m1, strTrimSanitizeUnescapeF)
      .transform [Option[URL]] (
        {rawOpt =>
          rawOpt.flatMap { raw =>
            val url1 = urlNormalizeSafe(raw)
            val url2 = if (url1.isEmpty) raw else url1
            try {
              Some(new URL(url2))
            } catch {
              case ex: MalformedURLException => None
            }
          }
        },
        { _.map(UrlUtil.humanizeUrl) }
      )
  }
  /** Маппер form-поля с ссылкой в java.net.URL. */
  def urlM: Mapping[URL] = {
    urlOptM
      .verifying("error.required", _.isDefined)
      .transform[URL] (_.get, Some.apply)
  }


  // Маппер для float-значений.
  val floatRe = "[-+]?\\d{0,8}([,.]\\d{0,7})?".r
  def floatM = nonEmptyText(maxLength = 15)
    .verifying("float.invalid", floatRe.pattern.matcher(_).matches())
    .transform[Float](_.replace(',', '.').toFloat, _.toString)

  // Маппер для полноценных double значений для floating point чисел в различных представлениях.
  private def doubleRe = """-?(\d+([.,]\d*)?|\d*[.,]\d+)([eE][+-]?\d+)?[fFdD]?""".r
  def doubleM = nonEmptyText(maxLength = 32)
    .verifying("float.invalid", doubleRe.pattern.matcher(_).matches())
    .transform[Double](
      { _.replace(',', '.').toDouble},
      { _.toString }
    )

  /** Form mapping для LocalDate. */
  def localDateM = text(maxLength = 32)
    .transform[Option[LocalDate]](
      {raw =>
        DateParseUtil.extractDates(raw)
          .headOption
      },
      {ldOpt =>
        ldOpt.fold(""){ DateTimeUtil.simpleLocalDateFmt.format }
      }
    )
    .verifying("error.required", _.isDefined)
    .transform[LocalDate](_.get, Some.apply)


  // География
  /** Маппинг для элемента NodeGeoLevels. */
  def nodeGeoLevelM: Mapping[MNodeGeoLevel] = {
    nonEmptyText(minLength = 1, maxLength = 5)
      .transform[Option[MNodeGeoLevel]] (
        { s =>
          Option(s.trim)
            .filter(!_.isEmpty)
            .flatMap(MNodeGeoLevels.withNameOption)
        },
        { _.fold("")(_.esfn) }
      )
      .verifying("error.required", _.isDefined)
      .transform [MNodeGeoLevel] (_.get, Some.apply)
  }

  def currencyOptM: Mapping[Option[MCurrency]] = {
    text(minLength = 3, maxLength = 3)
      .transform[String](_.toUpperCase, identity)
      .transform[Option[MCurrency]](
        MCurrencies.withValueOpt,
        _.fold("")(_.currencyCode)
      )
  }
  def currencyM: Mapping[MCurrency] = {
    currencyOptM
      .verifying("error.currency.code", _.nonEmpty)
      .transform[MCurrency]( EmptyUtil.getF, EmptyUtil.someF )
  }
  def currencyOrDfltM: Mapping[MCurrency] = {
    default(currencyM, MCurrencies.default)
  }


  // Валюты по ISO 4217. Там целые 0..~1000.
  // Но в яндекс-кассе в демо-режиме используются "демо-рубли", которые имеют код 10643. Возможно, у других ПС похожий прикол бывает.
  def currencyOpt_iso4217(offset: Int): Mapping[Option[MCurrency]] = {
    val m0 = number(min = offset, max = offset + 9999)
    val m1 = if (offset == 0) {
      m0
    } else {
      m0.transform[Int](_ - offset, _ + offset)
    }
    m1.transform[Option[MCurrency]](
      MCurrencies.withIso4217Option,
      _.getOrElse(MCurrencies.default).iso4217
    )
  }
  def currencyOpt_iso4217: Mapping[Option[MCurrency]] = {
    currencyOpt_iso4217(0)
  }
  def currency_iso4217: Mapping[MCurrency] = {
    currency_iso4217(0)
  }
  def currency_iso4217(offset: Int): Mapping[MCurrency] = {
    currencyOpt_iso4217(offset)
      .verifying("error.required", _.nonEmpty)
      .transform[MCurrency](EmptyUtil.getF, EmptyUtil.someF)
  }

  /** Маппер для lat-lon координат, заданных в двух полях формы.
    * val потому что некоторые XFormUtil юзают это как val. */
  val geoPointM: Mapping[MGeoPoint] = {
    val vMapper = doubleM
    mapping(
      "lat" -> vMapper,
      "lon" -> vMapper
    )
    { MGeoPoint.fromDouble }
    { MGeoPoint.unapplyDouble }
  }


  /** Копипаст из FormUrlEncodedParser. Распарсить строку qs-аргументов в последовательность. */
  def parseToPairs(data: String, encoding: String = "utf-8"): Iterator[(String, String)] = {
    import java.net._
    // Generate all the pairs, with potentially redundant key values, by parsing the body content.
    data.split('&')
      .iterator
      .flatMap { param =>
        if (param.contains("=") && !param.startsWith("=")) {
          val parts = param.split("=")
          val key = URLDecoder.decode(parts.head, encoding)
          val value = URLDecoder.decode(parts.tail.headOption.getOrElse(""), encoding)
          Seq(key -> value)
        } else {
          Nil
        }
      }
  }

  def parseQsToMap(data: String, encoding: String = "utf-8"): Map[String, Seq[String]] = {
    parseToPairs(data, encoding)
      .toSeq
      .groupBy(_._1)
      .view
      .mapValues { _.map { _._2 } }
      .toMap
  }


  /** Маппер опционального языка интерфейса suggest.io */
  def uiLangOptM(langCurr: Option[Lang] = None): Mapping[Option[Lang]] = {
    text(maxLength = 5)
      .transform [Option[Lang]] (Lang.get, _.orElse(langCurr).getOrElse(Lang.defaultLang).code)
  }
  /** Маппер языка интерфейса. */
  def uiLangM(langCurr: Option[Lang] = None): Mapping[Lang] = {
    uiLangOptM(langCurr)
      .verifying("error.required", _.isDefined)
      .transform [Lang] (_.get, Some.apply)
  }


  /** Маппер для версии документа elasticsearch. */
  def esVsnM: Mapping[Long] = {
    longNumber(min = 0L)
  }

  /** Опциональный маппер для ES-версии. */
  def esVsnOptM: Mapping[Option[Long]] = {
    optional(esVsnM)
  }

  /** Неквантуемый коэффициент масштабирования в разумных пределах. */
  def szMultM: Mapping[SzMult_t] = {
    floatM
      .verifying("e.sz.mult.too.low", _ >= 0.25F)
      .verifying("e.sz.mult.too.high", _ <= 10F)
  }

  /** Опциональный маппинг предиката. */
  def predicateOptM: Mapping[Option[MPredicate]] = {
    optional(
      nonEmptyText(minLength = 1, maxLength = 10)
        .transform(strTrimSanitizeF, strIdentityF)
    )
    .transform [Option[MPredicate]] (
      _.flatMap(MPredicates.withValueOpt),
      _.map(_.value)
    )
  }

  /** Обязательный маппинг предиката. */
  def predicateM: Mapping[MPredicate] = {
    predicateOptM
      .verifying("error.required", _.nonEmpty)
      .transform [MPredicate] ( EmptyUtil.getF, EmptyUtil.someF )
  }

}

