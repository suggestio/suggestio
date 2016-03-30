package util

import java.net.{MalformedURLException, URL}
import java.util.Currency

import io.suggest.common.menum.{EnumMaybeWithId, EnumMaybeWithName, EnumValue2Val}
import io.suggest.model.geo.{CircleGs, Distance}
import io.suggest.model.n2.node.meta.colors.MColorData
import io.suggest.model.sc.common.LvlMap_t
import io.suggest.util.{DateParseUtil, UrlUtil, UuidUtil}
import io.suggest.ym.YmParsers
import io.suggest.ym.model.common.MImgSizeT
import models._
import models.blk.SzMult_t
import org.apache.commons.lang3.StringEscapeUtils
import org.elasticsearch.common.unit.DistanceUnit
import org.joda.time.format.ISOPeriodFormat
import org.joda.time.{DateTimeZone, LocalDate, Period}
import org.postgresql.util.PGInterval
import play.api.data.Forms._
import play.api.data.Mapping
import play.api.i18n.Lang
import util.HtmlSanitizer._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.13 19:01
 * Description: набор хелперов для форм.
 */
object FormUtil {

  val strIdentityF = {s:String => s}
  val strTrimF = {s: String => s.trim }
  def stripHtml(s: String) = stripAllPolicy.sanitize(s)
  val strTrimSanitizeF = {s:String =>
    // TODO Исключить двойные пробелы
    stripHtml(s).trim
  }
  val strTrimBrOnlyF = {s: String =>
    // TODO прогонять через HtmlCompressor
    brOnlyPolicy.sanitize(s).trim
  }
  val strTrimSanitizeLowerF = strTrimSanitizeF andThen { _.toLowerCase }
  val strFmtTrimF = {s: String =>
    // TODO прогонять через HtmlCompressor
    textFmtPolicy.sanitize(s).trim
  }

  val strUnescapeF = {s: String => StringEscapeUtils.unescapeHtml4(s) }

  /** sanitizer вызывает html escape для амперсандов, ковычек и т.д. Если это не нужно, то следует вызывать unescape ещё. */
  val strTrimSanitizeUnescapeF = strTrimSanitizeF andThen strUnescapeF

  private val crLfRe = "\r\n".r
  private val lfCrRe = "\n\r".r
  private val lfRe = "\n".r
  private val brReplacement = "<br/>"
  val replaceEOLwithBR = {s: String =>
    var r1 = crLfRe.replaceAllIn(s, brReplacement)
    r1 = lfCrRe.replaceAllIn(r1, brReplacement)
    lfRe.replaceAllIn(r1, brReplacement)
  }
  private val brTagRe = "(?i)<br\\s*/?\\s*>".r
  val replaceBRwithEOL = {s: String =>
    brTagRe.replaceAllIn(s, "\r\n")
  }

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
      case ex: Exception => false
    }
  }

  /** Маппер поля формы для опциональной time-зоны. */
  def timeZoneOptM: Mapping[Option[DateTimeZone]] = {
    val ntm = nonEmptyText(minLength = 2, maxLength = 64)
    toStrOptM(ntm, strTrimSanitizeF)
      .transform [Option[DateTimeZone]] (
        {tzRawOpt =>
          tzRawOpt.flatMap { tzRaw =>
            try {
              Option( DateTimeZone.forID(tzRaw) )
            } catch {
              case ex: Exception => None
            }
          }
        },
        { _.map(_.getID) }
      )
  }
  /** Маппер поля формы для обязательной time-зоны. */
  def timeZoneM: Mapping[DateTimeZone] = {
    timeZoneOptM
      .verifying("error.invalid", { _.isDefined })
      .transform [DateTimeZone] (_.get, Some.apply)
  }


  /** Регэксп для парсинга uuid, закодированного в base64. */
  val uuidB64Re = "[_a-zA-Z0-9-]{19,25}".r

  /** id'шники в ES-моделях генерятся силами ES. Тут маппер для полей, содержащих ES-id. */
  def esIdM = nonEmptyText(minLength=19, maxLength=30)
    .transform(strTrimSanitizeF, strIdentityF)
    .verifying("error.invalid.id", uuidB64Re.pattern.matcher(_).matches())

  /** Тоже самое, что и esIdM, но пытается декодировать UUID из id.
    * id, сгенеренные es, тут не прокатят! */
  def esIdUuidM = esIdM.verifying("error.invalid.uuid", UuidUtil.isUuidStrValid(_))

  /** Маппинг для номера этажа в ТЦ. */
  def floorM = nonEmptyText(maxLength = 4)
    .transform(strTrimSanitizeUnescapeF, strIdentityF)
  def floorOptM = optional(floorM)
    .transform [Option[String]] (emptyStrOptToNone, identity)

  /** Маппинг для секции в ТЦ. */
  def sectionM = nonEmptyText(maxLength = 6)
    .transform(strTrimSanitizeUnescapeF, strIdentityF)
  def sectionOptM = optional(sectionM)
    .transform [Option[String]] (emptyStrOptToNone, identity)

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
    nonEmptyText(maxLength = 10)
      .transform[Option[AdnShownType]]( AdnShownTypes.maybeWithName, _.fold("")(_.toString) )
  }
  def adnShownTypeIdOptM: Mapping[Option[String]] = {
    adnShownTypeOptM
      .transform[Option[String]](_.map(_.toString), _.flatMap(AdnShownTypes.maybeWithName))
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

  /** Опциональная дистанция, заданная значением и единицами измерения. Используется вместо optional([[distanceM]])
    * т.к. units задаются select'ом, т.е. всегда присутсвуют. Что порождает ошибки при биндинге. */
  def distanceOptM: Mapping[Option[Distance]] = {
    mapping(
      "value" -> optional(doubleM),
      "units" -> optional(distanceUnitsM)
    )
    {(valueOpt, unitsOpt) =>
      valueOpt.flatMap { distance =>
        unitsOpt.map { units =>
          Distance(distance, units)
        }
      }
    }
    {distanceOpt =>
      val valueOpt = distanceOpt.map(_.distance)
      val unitsOpt = distanceOpt.map(_.units)
      Some((valueOpt, unitsOpt))
    }
  }

  /** Маппинг для географического круга, задаваемого географическим центром и радиусом. */
  def circleM: Mapping[CircleGs] = {
    mapping(
      "center"  -> geoPointM,
      "radius"  -> distanceM
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
  def toSomeStrM(x: Mapping[String], trimmer: String => String = strTrimSanitizeUnescapeF): Mapping[Option[String]] = {
    toStrOptM(x, trimmer)
     .verifying("error.required", _.isDefined)
  }


  val nameM = nonEmptyText(maxLength = 64)
    .transform(strTrimSanitizeUnescapeF, strIdentityF)
  def nameOptM = optional(nameM)


  /** Маппер для поля, содержащего код цвета. */
  private val colorCheckRE = "(?i)[a-f0-9]{6}".r
  def colorM = nonEmptyText(minLength = 6, maxLength = 6)
    .verifying("error.color.invalid", colorCheckRE.pattern.matcher(_).matches)
  def colorOptM = optional(colorM)
    .transform [Option[String]] (emptyStrOptToNone, identity)
  def colorSomeM = toSomeStrM(colorM)

  def colorDataM = colorM.transform [MColorData] (MColorData.apply, _.code)

  private def _color2dataOptM(m0: Mapping[Option[String]]): Mapping[Option[MColorData]] = {
    m0.transform [Option[MColorData]] (
      _.map(MColorData.apply),
      _.map(_.code)
    )
  }
  def colorDataOptM  = _color2dataOptM(colorOptM)
  def colorDataSomeM = _color2dataOptM(colorSomeM)

  def publishedTextM = text(maxLength = 2048)
    .transform(strFmtTrimF, strIdentityF)
  def publishedTextOptM = optional(publishedTextM)
    .transform [Option[String]] (emptyStrOptToNone, identity)

  def townM = nonEmptyText(maxLength = 32)
    .transform(strTrimSanitizeUnescapeF, strIdentityF)
  def townOptM = optional(townM)
    .transform [Option[String]] (emptyStrOptToNone, identity)
  def townSomeM = toSomeStrM(townM)

  def addressM = nonEmptyText(minLength = 6, maxLength = 128)
    .transform(strTrimSanitizeUnescapeF, strIdentityF)
  def addressOptM = optional(addressM)
    .transform [Option[String]] (emptyStrOptToNone, identity)
  def addressSomeM = toSomeStrM(addressM)


  // TODO Нужен нормальный валидатор телефонов.
  def phoneM = nonEmptyText(minLength = 5, maxLength = 50)
    .transform(strTrimSanitizeUnescapeF, strIdentityF)
  def phoneOptM = optional(phoneM)
    .transform [Option[String]] (emptyStrOptToNone, identity)
  //def phoneSomeM = toSomeStrM(phoneM)

  /** Маппер для человеческого траффика, заданного числом. */
  def humanTrafficAvgM = number(min = 10)

  def text2048M: Mapping[String] = {
    text(maxLength = 2048)
      .transform(strTrimSanitizeF, strIdentityF)
  }
  def audienceDescrM = text2048M

  // Трансформеры для optional-списков.
  def optList2ListF[T] = { optList: Option[List[T]] => optList getOrElse Nil }
  def list2OptListF[T] = { l:List[T] =>  if (l.isEmpty) None else Some(l) }

  /** Маппер form-поля URL в строку URL */
  def urlNormalizeSafe(s: String): String = {
    try {
      UrlUtil.normalize(s)
    } catch {
      case ex: Exception => ""
    }
  }
  def urlStrM = nonEmptyText(minLength = 8)
    .transform[String](strTrimF andThen urlNormalizeSafe, UrlUtil.humanizeUrl)
    .verifying("mappers.url.invalid_url", isValidUrl(_))
  def urlStrOptM = optional(urlStrM)

  /** Толерантный к проблемам маппинг ссылки. */
  def urlStrOptTolerantM: Mapping[Option[String]] = {
    optional(text)
      // TODO Как-то хреново отрабатывается тут ссылка. Если нет "http://" вначале, что всё, катастрофа и None.
      .transform[Option[String]] (
        {sOpt =>
          sOpt.flatMap { s =>
            val st = strTrimSanitizeF(s)
            if (isValidUrl(st))
              Some(st)
            else
              None
          }
        },
        identity
      )
  }

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
  def doubleRe = """-?(\d+([.,]\d*)?|\d*[.,]\d+)([eE][+-]?\d+)?[fFdD]?""".r
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
        ldOpt.fold(""){ DateTimeUtil.simpleLocalDateFmt.print }
      }
    )
    .verifying("error.required", _.isDefined)
    .transform[LocalDate](_.get, Some.apply)

  /** ISO-период в виде стандартной строки P1Y3M... */
  def isoPeriodM: Mapping[Period] = {
    import YmParsers.{ISO_PERIOD_PARSER, parse}
    val formatter = ISOPeriodFormat.standard()
    text(minLength = 3, maxLength = 64)
      .transform[Option[Period]](
        { str => Option(parse(ISO_PERIOD_PARSER, str).getOrElse(null)) },
        { case Some(period) => period.toString(formatter)
          case None => "" }
      )
      .verifying("error.invalid", _.isDefined)
      .transform[Period](_.get, Some.apply)
  }


  // География
  /** Маппинг для элемента NodeGeoLevels. */
  def nodeGeoLevelM: Mapping[NodeGeoLevel] = {
    nonEmptyText(minLength = 1, maxLength = 5)
      .transform[Option[NodeGeoLevel]] (
        { s => Option(s.trim).filter(!_.isEmpty).flatMap(NodeGeoLevels.maybeWithName) },
        { _.fold("")(_.esfn) }
      )
      .verifying("error.required", _.isDefined)
      .transform [NodeGeoLevel] (_.get, Some.apply)
  }

  // До web21:8e3432fbf693 включительно здесь жили маппинги цены.


  def pgIntervalPretty(pgi: PGInterval) = {
    pgi.toString.replaceAll("0([.,]0+)?\\s+[a-z]+", "").trim
  }

  def currencyCodeM: Mapping[String] = {
    text(minLength = 3, maxLength = 3)
      .transform[String](_.toUpperCase, identity)
      .verifying("error.currency.code", {cc =>
        try {
          Currency.getInstance(cc)
          true
        } catch {
          case ex: Exception => false
        }
      })
  }
  def currencyCodeOrDfltM: Mapping[String] = {
    default(currencyCodeM, CurrencyCodeOpt.CURRENCY_CODE_DFLT)
  }


  /** Маппер для lat-lon координат, заданных в двух полях формы.
    * val потому что некоторые XFormUtil юзают это как val. */
  val geoPointM: Mapping[GeoPoint] = {
    mapping(
      "lat" -> doubleM,
      "lon" -> doubleM
    )
    { GeoPoint.apply }
    { GeoPoint.unapply }
  }

  /** Опциональный маппер для lat-lon координат. */
  def geoPointOptM: Mapping[Option[GeoPoint]] = {
    optional(geoPointM)
  }


  def slsStrM: Mapping[LvlMap_t] = {
    text(maxLength = 256)
      .transform[LvlMap_t](
        {raw =>
          raw.split("\\s*,\\s*")
            .toSeq
            .map { one =>
              val Array(slRaw, slMaxRaw) = one.split("\\s*=\\s*")
              val sl: AdShowLevel = AdShowLevels.withName(slRaw)
              sl -> slMaxRaw.toInt
            }
            .filter(_._2 > 0)
            .toMap
        },
        {sls =>
          val raws = sls.map {
            case (sl, slMax)  =>  s"${sl.name} = $slMax"
          }
          raws.mkString(", ")
        }
      )
  }

  /** Маппер размеров, заданные через width и height. Например, размеры картинки. */
  def whSizeM: Mapping[MImgSizeT] = {
    val sideM = number(min = 10, max = 2048)
    mapping(
      "height" -> sideM,
      "width"  -> sideM
    )
    {(height, width) =>
      MImgInfoMeta(height = height, width = width)  : MImgSizeT
    }
    {mim =>
      Some((mim.height, mim.height))
    }
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
      .mapValues { _.map { _._2 } }
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

  def szMultOptM: Mapping[Option[SzMult_t]] = {
    optional(floatM)
  }


  /** Базовый трейт для сборки более конкретных трейтов-аддонов form-маппингов к моделям scala.Enumeration. */
  sealed trait EnumFormMapping extends EnumValue2Val {

    /** Черновой маппинг узнавания экземпляра модели. */
    protected def mappingOptDirty: Mapping[Option[T]]

    /** Обязательный form mapping. */
    def mapping: Mapping[T] = {
      mappingOptDirty
        .verifying("error.required", _.isDefined)
        .transform(_.get, Some.apply)
    }

    /** Опциональный form mapping. */
    def mappingOpt: Mapping[Option[T]] = {
      optional(mappingOptDirty)
        .transform(_.flatten, Some.apply)
    }
  }

  /** Быстрое добавление типовых Form mapping'ов в scala.Enumeration-модели.
    * Экземпляры модели-реализации должны иметь перезаписанный toString(), возвращающий id экземлпяра.
    * Либо, можно перезаписать strIdOf(). */
  trait StrEnumFormMappings extends EnumMaybeWithName with EnumFormMapping {

    /** Извлечение строкового id из экземпляра модели. */
    protected def strIdOf(v: T): String = v.toString

    protected def _idMinLen: Int = 0
    protected def _idMaxLen: Int = 32

    protected def mappingOptDirty: Mapping[Option[T]] = {
      text(minLength = _idMinLen, maxLength = _idMaxLen)
        .transform [Option[T]] (
          strTrimSanitizeF andThen maybeWithName,
          _.fold("")(strIdOf)
        )
    }

  }

  /** Быстрое добавления form mapping в scala.Enumeration-модели, использующих целочисленную адресацию. */
  trait IdEnumFormMappings extends EnumFormMapping with EnumMaybeWithId {

    /** Извлечение целочисленного идентификатора экземпляра модели. */
    protected def intIdOf(v: T): Int = v.id

    protected def _idMin = 0
    protected def _idMax = Int.MaxValue

    override protected def mappingOptDirty: Mapping[Option[T]] = {
      number(min = _idMin, max = _idMax)
        .transform [Option[T]] (maybeWithId, _.fold(Int.MinValue)(intIdOf))
    }

    def idMapping: Mapping[Int] = {
      mapping
        .transform[Int](_.id, apply)
    }
  }

}

