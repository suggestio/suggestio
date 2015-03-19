package util

import io.suggest.model.geo.{CircleGs, Distance, GeoPoint}
import io.suggest.ym.model.NodeGeoLevels
import io.suggest.ym.model.common.AdnMemberShowLevels.LvlMap_t
import io.suggest.ym.model.common.MImgSizeT
import models.stat.{ScStatActions, ScStatAction}
import org.apache.commons.lang3.StringEscapeUtils
import org.elasticsearch.common.unit.DistanceUnit
import play.api.data.Forms._
import java.net.{MalformedURLException, URL}
import io.suggest.util.{UuidUtil, JacksonWrapper, DateParseUtil, UrlUtil}
import gnu.inet.encoding.IDNA
import HtmlSanitizer._
import play.api.data.Mapping
import org.joda.time.{DateTimeZone, Period, LocalDate}
import io.suggest.ym.YmParsers
import org.joda.time.format.ISOPeriodFormat
import models._
import org.postgresql.util.PGInterval
import java.sql.SQLException
import java.io._
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import org.apache.commons.codec.binary.{Base64InputStream, Base64OutputStream}
import scala.collection.GenTraversableOnce
import java.util.Currency

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.13 19:01
 * Description: набор хелперов для форм.
 */
object FormUtil {

  val strIdentityF = {s:String => s}
  val strTrimF = {s: String => s.trim }
  val strTrimSanitizeF = {s:String =>
    // TODO Исключить двойные пробелы
    stripAllPolicy.sanitize(s).trim
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

  private def allowedProtocolRE = "(?i)https?".r

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
  def adnShownTypeM: Mapping[AdnShownType] = {
    adnShownTypeOptM
      .verifying("error.required", _.isDefined)
      .transform[AdnShownType](_.get, Some.apply)
  }
  def adnShownTypeIdM: Mapping[String] = {
    adnShownTypeM
      .transform[String](_.toString, AdnShownTypes.withName)
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
      "center"  -> latLng2geopointM,
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


  /** Возвращение проверенного пароля как Some(). */
  def passwordWithConfirmSomeM = toStrOptM(passwordWithConfirmM)


  val nameM = nonEmptyText(maxLength = 64)
    .transform(strTrimSanitizeUnescapeF, strIdentityF)
  def companyNameM = nameM


  /** Маппер для поля, содержащего код цвета. */
  private val colorCheckRE = "(?i)[a-f0-9]{6}".r
  val colorM = nonEmptyText(minLength = 6, maxLength = 6)
    .verifying("error.color.invalid", colorCheckRE.pattern.matcher(_).matches)
  val colorOptM = optional(colorM)
    .transform [Option[String]] (emptyStrOptToNone, identity)
  def colorSomeM = toSomeStrM(colorM)

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


  /** id категорий. */
  def adCatIdsM: Mapping[Set[String]] = {
    list(esIdM).transform [Set[String]] (
      {_.iterator
        .flatMap { v =>
          val v1 = strTrimSanitizeF(v)
          if (v1.isEmpty) Seq.empty else Seq(v1)
        }
        .toSet
      },
      { _.toList }
    )
  }
  def adCatIdsNonEmptyM = adCatIdsM.verifying("error.required", _.nonEmpty)

  // TODO Нужен нормальный валидатор телефонов.
  def phoneM = nonEmptyText(minLength = 5, maxLength = 50)
    .transform(strTrimSanitizeUnescapeF, strIdentityF)
  def phoneOptM = optional(phoneM)
    .transform [Option[String]] (emptyStrOptToNone, identity)
  def phoneSomeM = toSomeStrM(phoneM)

  /** Маппер для человеческого траффика, заданного числом. */
  def humanTrafficAvgM = number(min = 10)

  val text2048M = text(maxLength = 2048).transform(strTrimSanitizeF, strIdentityF)
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

  /** Проверить ссылку на возможность добавления сайта в индексацию. */
  def urlAllowedMapper = urlM
    .verifying("mappers.url.only_http_https_allowed", { url =>
      allowedProtocolRE.pattern.matcher(url.getProtocol).matches()
    })
    .verifying("mappers.url.hostname_prohibited", { url =>
      UrlUtil.isHostnameValid(url.getHost)
    })


  // Маппер домена. Формат ввода тут пока не проверяется.
  def domainMapper = nonEmptyText(minLength = 4, maxLength = 128)
    .transform(strTrimSanitizeLowerF, strIdentityF)
    .verifying("mappers.url.hostname_prohibited", UrlUtil.isHostnameValid(_))

  // Маппер домена с конвертором в dkey.
  def domain2dkeyMapper = domainMapper
    .transform [String] (UrlUtil.normalizeHostname, IDNA.toUnicode)

  // Маппер для float-значений.
  val floatRe = "[-+]?\\d{0,8}([,.]\\d{0,4})?".r
  def floatM = nonEmptyText(maxLength = 15)
    .verifying("float.invalid", floatRe.pattern.matcher(_).matches())
    .transform[Float](_.toFloat, _.toString)

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
    import YmParsers.{parse, ISO_PERIOD_PARSER}
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


  // Ценовые значения

  import io.suggest.ym.parsers.Price
  import UserInputParsers._


  /** Макс.длина текста в поле цены. */
  val PRICE_M_MAX_STRLEN = 40

  /** Нестрогий маппинг цены. Ошибка будет только если слишком много букв. */
  def priceM: Mapping[(String, Option[Price])] = {
    text
      .transform[(String, Option[Price])](
        {raw =>
          val raw0 = if (raw.length > PRICE_M_MAX_STRLEN)
            raw.substring(0, PRICE_M_MAX_STRLEN)
          else
            raw
          val raw1 = strTrimSanitizeF(raw0)
          raw1 -> parsePrice(raw1) },
        { case (raw, None) => raw
          case (raw, Some(_)) if !raw.isEmpty => raw
          case (_, Some(price)) =>
            val p = price.price
            price.currency.getCurrencyCode match {
              case "RUB" => p.toString
              case "USD" => "$" + p
              case cCode => p + " " + cCode
          }
        }
      )
  }

  /** Маппер типа тарифа. */
  def bTariffTypeM: Mapping[BTariffType] = {
    nonEmptyText(maxLength = 1)
      .transform[Option[BTariffType]](
        { BTariffTypes.maybeWithName },
        { case Some(tt) => tt.toString
          case None => "" }
      )
      .verifying("error.invalid", _.isDefined)
      .transform({ _.get }, Some.apply)
  }

  def pgIntervalPretty(pgi: PGInterval) = pgi.toString.replaceAll("0([.,]0+)?\\s+[a-z]+", "").trim

  def pgIntervalM: Mapping[PGInterval] = {
    nonEmptyText(minLength = 3, maxLength = 256)
      .transform[Option[PGInterval]](
        {str =>
          try {
            Some(new PGInterval(str))
          } catch {
            case ex: SQLException => None
          }
        },
        { case Some(pgi) => pgIntervalPretty(pgi)
          case None      => "" }
      )
      .verifying("error.invalid", _.isDefined)
      .transform(_.get, Some.apply)
  }

  def pgIntervalStrM: Mapping[String] = {
    pgIntervalM.transform[String](
      _.toString,
      {str => new PGInterval(str) }
    )
  }
  

  /** Маппинг для задания цены. Либо цена, либо ошибка. Тащим исходное значение с собой
    * для возможности быстрого доступа к нему из маппинга без помощи локали клиента и т.д. */
  def priceStrictM: Mapping[(String, Price)] = {
    priceM
      .verifying("error.required", _._2.isDefined)
      .transform[(String, Price)](
        {case (raw, pOpt) => raw -> pOpt.get},
        {case (raw, p) => raw -> Some(p)}
      )
      .verifying("error.price.mustbe.nonneg", { _._2.price >= 0F })
      .verifying("error.price.too.much", { _._2.price < 100000000F })
  }

  /** price, но без исходного raw-значения. */
  def priceStrictNoraw: Mapping[Price] = {
    priceStrictM.transform[Price](
      { case (raw, price) => price },
      { price => "" -> price }
    )
  }


  def adhocPercentFmt(pc: Float) = TplDataFormatUtil.formatPercentRaw(pc) + "%"

  /** Макс.длина текста в полях price/percent. Используется в маппинге и в input-шаблонах редактора блоков. */
  val PERCENT_M_CHARLEN_MAX = 32

  // Процентные значения
  /** Нестрогий маппер процентов. Крэшится только если слишком много букв. */
  def percentM = {
    text.transform[(String, Option[Float])](
      {raw =>
        val raw0 = if (raw.length > PERCENT_M_CHARLEN_MAX)
          raw.substring(0, PERCENT_M_CHARLEN_MAX)
        else
          raw
        val raw1 = strTrimSanitizeF(raw0)
        raw1 -> parsePercents(raw1)
      },
      { case (raw, opt) if !raw.isEmpty || opt.isEmpty => raw
        case (raw, Some(pc)) => adhocPercentFmt(pc) }
    )
  }

  /** Маппинг со строгой проверкой на проценты. */
  def getStrictDiscountPercentM(min: Float, max: Float): Mapping[Float] = {
    percentM
      .verifying("error.required", _._2.isDefined)
      .verifying("error.discount.too.large", { _._2.get >= min })
      .verifying("error.discount.too.small", { _._2.get <= max })
      .transform[Float](
        _._2.get,
        {pc => adhocPercentFmt(pc) -> Some(pc)}
      )
  }

  /** Маппинг, толерантный к значениям, выходящим за пределы диапазона. */
  def getTolerantDiscountPercentM(min: Float, max: Float, dflt: Float): Mapping[Float] = {
    percentM.transform[Float](
      {case (raw, pcOpt) =>
        pcOpt
          .map { pcf => Math.min(max, Math.max(min, pcf)) }
          .getOrElse(dflt)
      },
      {pcf =>
        val raw = TplDataFormatUtil.formatPercentRaw(pcf) + "%"
        raw -> Some(pcf)
      }
    )
  }


  /** Маппинг для задания причины при сокрытии сущности. */
  def hideEntityReasonM = nonEmptyText(maxLength = 512)
    .transform(strTrimSanitizeUnescapeF, strIdentityF)


  /** Маппер типа adn-узла. */
  def adnMemberTypeM: Mapping[AdNetMemberType] = nonEmptyText(maxLength = 1)
    .transform [Option[AdNetMemberType]] (
      { AdNetMemberTypes.maybeWithName },
      { _.map(_.name).getOrElse("") }
    )
    .verifying("error.required", _.isDefined)
    .transform [AdNetMemberType] (_.get, Some.apply)


  def adStatActionM: Mapping[ScStatAction] = {
    nonEmptyText(maxLength = 1)
      .transform[ScStatAction](ScStatActions.withName, _.toString)
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


  /** Маппер для lat-lon координат, заданных в двух полях формы. */
  def latLng2geopointM: Mapping[GeoPoint] = {
    mapping(
      "lat" -> doubleM,
      "lon" -> doubleM
    )
    { GeoPoint.apply }
    { GeoPoint.unapply }
  }

  /** Опциональный маппер для lat-lon координат. */
  def latLng2geopointOptM: Mapping[Option[GeoPoint]] = {
    optional(latLng2geopointM)
  }


  /** Маппер для поля, содержащего имя AdnSink. */
  def sinkM: Mapping[AdnSink] = {
    nonEmptyText(minLength = 1, maxLength = 1)
      .transform [Option[AdnSink]] (
        { AdnSinks.maybeWithName },
        { _.getOrElse(AdnSinks.default).name }
      )
      .verifying("error.required", _.isDefined)
      .transform [AdnSink] (_.get, Some.apply)
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

}


/** Сериализатор данных в поле формы, пригодное для передачи на клиент и возврату обратно.
  * Структура отражает Map[String, String] на Json + gzip + base64.
  * {"ad.offer.text1.value" : "рекламо", "ad.offer.text1.color" : "FFFFFF", ...}.
  */
object FormDataSerializer extends PlayLazyMacroLogsImpl {
  import play.api.libs.json._
  import LOGGER._

  val ENCODING = "ISO-8859-1"

  /** Сериализация Form.data или любой другой совместимой коллекции. */
  def serializeData(data: GenTraversableOnce[(String, String)]): String = {
    // Нано-оптимизация: вместо mapValues() + toSeq() намного эффективнее юзать foldLeft[List]
    val dataJs = data.foldLeft[List[(String, JsValue)]] (Nil) {
      case (acc, (k, v))  =>  k -> JsString(v) :: acc
    }
    val jsonStr = JsObject(dataJs).toString()
    // Сжимаем
    compress(jsonStr)
  }

  /** Десериализация выхлопа serializeData(). Для упрощения используется jackson. */
  def deserializeData(s: String): Map[String, String] = {
    val jsonStream = decompressStream(s)
    try {
      JacksonWrapper.deserialize[Map[String, String]](jsonStream)
    } finally {
      jsonStream.close()
    }
  }

  def deserializeDataSafe(s: String): Option[Map[String, String]] = {
    try {
      Some(deserializeData(s))
    } catch {
      case ex: Exception =>
        if (LOGGER.underlying.isDebugEnabled) {
          val sb = new StringBuilder("deserializeDataSafe(): Failed to deser. string[")
            .append(s.length)
            .append(" chars]")
          if (LOGGER.underlying.isTraceEnabled) {
            sb.append(": ").append(s)
          }
          debug(sb.toString(), ex)
        }
        None
    }
  }

  /** Сжатие строки в gzip+base64. */
  def compress(str: String): String = {
    if (str == null || str.length() == 0) {
      ""
    } else {
      val out = new ByteArrayOutputStream()
      val b64 = new Base64OutputStream(out, true, 0, Array())
      val gzipped = new GZIPOutputStream(b64)
      // TODO Opt: Надо бы использовать url-safe кодирование и отбрасывать padding в хвосте.
      gzipped.write(str.getBytes)
      gzipped.close()
      out.toString(ENCODING)
    }
  }

  /** Декомпрессия потока. */
  def decompressStream(str: String): InputStream = {
    val bais = new ByteArrayInputStream(str.getBytes(ENCODING))
    val gis64 = new Base64InputStream(bais)
    new GZIPInputStream(gis64)
  }

  /** Разжатие строки из gzip+base64 строки. */
  def decompress(str: String): String = {
    if (str == null || str.length() == 0) {
      ""
    } else {
      val gis = decompressStream(str)
      try {
        val br = new BufferedReader(new InputStreamReader(gis, ENCODING))
        Stream.continually(br.readLine()).takeWhile(_ != null).mkString("")
      } finally {
        gis.close()
      }
    }
  }

}
