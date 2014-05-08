package util

import play.api.data.Forms._
import java.net.URL
import io.suggest.util.{JacksonWrapper, DateParseUtil, UrlUtil}
import gnu.inet.encoding.IDNA
import HtmlSanitizer._
import views.html.helper.FieldConstructor
import views.html.market.lk._
import play.api.data.Mapping
import org.joda.time.{Period, LocalDate}
import io.suggest.ym.YmParsers
import org.joda.time.format.ISOPeriodFormat
import models._
import org.postgresql.util.PGInterval
import java.sql.SQLException
import java.io._
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import org.apache.commons.codec.binary.{Base64InputStream, Base64OutputStream}
import scala.collection.GenTraversableOnce
import scala.Some
import play.api.Logger

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

  /** Функция, которая превращает Some("") в None. */
  def emptyStrOptToNone(s: Option[String]) = {
    s.filter(!_.isEmpty)
  }

  private val allowedProtocolRE = "(?i)https?".r

  def isValidUrl(urlStr: String): Boolean = {
    try {
      new URL(urlStr)
      true

    } catch {
      case ex:Throwable => false
    }
  }

  /** id'шники в ES-моделях генерятся силами ES. Тут маппер для полей, содержащих ES-id. */
  val esIdM = nonEmptyText(minLength=6, maxLength=64)
    .transform(strTrimSanitizeF, strIdentityF)

  /** Маппинг для номера этажа в ТЦ. */
  val floorM = nonEmptyText(maxLength = 4)
    .transform(strTrimSanitizeF, strIdentityF)
  val floorOptM = optional(floorM)
    .transform [Option[String]] (emptyStrOptToNone, identity)

  /** Маппинг для секции в ТЦ. */
  val sectionM = nonEmptyText(maxLength = 6)
    .transform(strTrimSanitizeF, strIdentityF)
  val sectionOptM = optional(sectionM)
    .transform [Option[String]] (emptyStrOptToNone, identity)

  /** Парсим текст, введённый в поле с паролем. */
  val passwordM = nonEmptyText
    .verifying("password.too.short", {_.length > 5})
    .verifying("password.too.long", {_.length <= 1024})

  /** Два поля: пароль и подтверждение пароля. Используется при регистрации пользователя. */
  val passwordWithConfirmM = tuple(
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


  def strOptGetOrElseEmpty(x: Option[String]) = x getOrElse ""
  def toStrOptM(x: Mapping[String]): Mapping[Option[String]] = {
    x.transform[Option[String]](Option.apply, strOptGetOrElseEmpty)
  }


  /** Возвращение проверенного пароля как Some(). */
  val passwordWithConfirmSomeM = toStrOptM(passwordWithConfirmM)


  val nameM = nonEmptyText(maxLength = 64)
    .transform(strTrimSanitizeF, strIdentityF)
  def shopNameM = nameM
  def martNameM = nameM
  def companyNameM = nameM


  /** Маппер для поля, содержащего код цвета. */
  // TODO Нужно добавить верификацию тут какую-то. Например через YmColors.
  val colorM = nonEmptyText(minLength = 6, maxLength = 6)
  val colorOptM = optional(colorM)
    .transform [Option[String]] (emptyStrOptToNone, identity)

  val publishedTextM = text(maxLength = 2048)
    .transform(strFmtTrimF, strIdentityF)
  val publishedTextOptM = optional(publishedTextM)
    .transform [Option[String]] (emptyStrOptToNone, identity)

  val townM = nonEmptyText(maxLength = 32)
    .transform(strTrimSanitizeF, strIdentityF)
  val townOptM = optional(townM)
    .transform [Option[String]] (emptyStrOptToNone, identity)

  val addressM = nonEmptyText(minLength = 10, maxLength = 128)
    .transform(strTrimSanitizeF, strIdentityF)
  val addressOptM = optional(addressM)
    .transform [Option[String]] (emptyStrOptToNone, identity)

  /** id категории. */
  def userCatIdM = esIdM
  val userCatIdOptM = optional(userCatIdM)
    .transform [Option[String]] (emptyStrOptToNone, identity)

  // TODO Нужен нормальный валидатор телефонов.
  val phoneM = nonEmptyText(minLength = 5, maxLength = 16)
  val phoneOptM = optional(phoneM)
    .transform [Option[String]] (emptyStrOptToNone, identity)

  def martAddressM = addressM

  // Трансформеры для optional-списков.
  def optList2ListF[T] = { optList: Option[List[T]] => optList getOrElse Nil }
  def list2OptListF[T] = { l:List[T] =>  if (l.isEmpty) None else Some(l) }

  /** Маппер form-поля URL в строку URL */
  val urlStrM = nonEmptyText(minLength = 8)
    .transform(strTrimF, strIdentityF)
    .verifying("mappers.url.invalid_url", isValidUrl(_))
  val urlStrOptM = optional(urlStrM)

  /** Маппер form-поля с ссылкой в java.net.URL. */
  val urlMapper = urlStrM
    .transform(new URL(_), {url:URL => url.toExternalForm})

  /** Проверить ссылку на возможность добавления сайта в индексацию. */
  val urlAllowedMapper = urlMapper
    .verifying("mappers.url.only_http_https_allowed", { url =>
      allowedProtocolRE.pattern.matcher(url.getProtocol).matches()
    })
    .verifying("mappers.url.hostname_prohibited", { url =>
      UrlUtil.isHostnameValid(url.getHost)
    })


  // Маппер домена. Формат ввода тут пока не проверяется.
  val domainMapper = nonEmptyText(minLength = 4, maxLength = 128)
    .transform(strTrimSanitizeLowerF, strIdentityF)
    .verifying("mappers.url.hostname_prohibited", UrlUtil.isHostnameValid(_))

  // Маппер домена с конвертором в dkey.
  val domain2dkeyMapper = domainMapper
    .transform(UrlUtil.normalizeHostname, {dkey:String => IDNA.toUnicode(dkey)})

  // Маппер для float-значений.
  val floatRe = "[0-9]{0,8}([,.][0-9]{0,4})?".r
  val floatM = nonEmptyText(maxLength = 15)
    .verifying("float.invalid", floatRe.pattern.matcher(_).matches())
    .transform(_.toFloat, {f: Float => f.toString})


  // Даты
  val localDate = text(maxLength = 32)
    .transform[Option[LocalDate]](
      DateParseUtil.extractDates(_).headOption,
      { ldOpt => ldOpt.map(_.toString) getOrElse "" }
    )
    .verifying("error.required", _.isDefined)
    .transform[LocalDate](_.get, Some.apply)

  /** ISO-период в виде стандартной строки P1Y3M... */
  val isoPeriodM: Mapping[Period] = {
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

  // Ценовые значения

  import io.suggest.ym.parsers.Price
  import UserInputParsers._

  /** Нестрогий маппинг цены. Ошибка будет только если слишком много букв. */
  val priceM: Mapping[(String, Option[Price])] = {
    text(maxLength = 40)
      .transform[(String, Option[Price])](
        {raw =>
          val raw1 = strTrimSanitizeF(raw)
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
  val bTariffTypeM: Mapping[BTariffType] = {
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

  val pgIntervalM: Mapping[PGInterval] = {
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

  val pgIntervalStrM: Mapping[String] = {
    pgIntervalM.transform[String](
      _.toString,
      {str => new PGInterval(str) }
    )
  }
  

  /** Маппинг для задания цены. Либо цена, либо ошибка. Тащим исходное значение с собой
    * для возможности быстрого доступа к нему из маппинга без помощи локали клиента и т.д. */
  val priceStrictM: Mapping[(String, Price)] = {
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
  val priceStrictNoraw: Mapping[Price] = {
    priceStrictM.transform[Price](
      { case (raw, price) => price },
      { price => "" -> price }
    )
  }


  def adhocPercentFmt(pc: Float) = TplDataFormatUtil.formatPercentRaw(pc) + "%"

  val PERCENT_M_CHARLEN_MAX = 32

  // Процентные значения
  /** Нестрогий маппер процентов. Крэшится только если слишком много букв. */
  val percentM = {
    text(maxLength = PERCENT_M_CHARLEN_MAX)
      .transform[(String, Option[Float])](
        {raw =>
          val raw1 = strTrimSanitizeF(raw)
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
  val hideEntityReasonM = nonEmptyText(maxLength = 512)
    .transform(strTrimSanitizeF, strIdentityF)


  /** Маппер типа adn-узла. */
  val adnMemberTypeM: Mapping[AdNetMemberType] = nonEmptyText(maxLength = 1)
    .transform [Option[AdNetMemberType]] (
      { AdNetMemberTypes.maybeWithName },
      { _.map(_.name).getOrElse("") }
    )
    .verifying("error.required", _.isDefined)
    .transform [AdNetMemberType] (_.get, Some.apply)

}


object FormHelpers {

  implicit val myFields = FieldConstructor(lkFieldConstructor.f)

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

  /** Десериализация выхлопа [[serializeData()]]. Для упрощения используется jackson. */
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
