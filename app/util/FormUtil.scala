package util

import play.api.data.Forms._
import java.net.URL
import io.suggest.util.UrlUtil
import gnu.inet.encoding.IDNA
import HtmlSanitizer._
import views.html.helper.FieldConstructor
import views.html.market.lk._
import play.api.data.Mapping

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
  val legalFloorM = nonEmptyText(maxLength = 4)

  /** Маппинг для секции в ТЦ. */
  val legalSectionM = nonEmptyText(maxLength = 6)

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

  /** Возвращение проверенного пароля как Some(). */
  val passwordWithConfirmSomeM = passwordWithConfirmM
    .transform({ Option.apply }, {pwOpt: Option[String] => pwOpt getOrElse ""})


  val nameM = nonEmptyText(maxLength = 64)
    .transform(strTrimSanitizeF, strIdentityF)
  def shopNameM = nameM
  def martNameM = nameM
  def companyNameM = nameM

  /** Маппер для поля, содержащего код цвета. */
  // TODO Нужно добавить верификацию тут какую-то. Например через YmColors.
  val colorM = nonEmptyText(maxLength = 16)

  val publishedTextM = text(maxLength = 2048)
    .transform(strFmtTrimF, strIdentityF)
  val publishedTextOptM = optional(publishedTextM)

  val townM = nonEmptyText(maxLength = 32)
    .transform(strTrimSanitizeF, strIdentityF)

  val addressM = nonEmptyText(minLength = 10, maxLength = 128)
    .transform(strTrimSanitizeF, strIdentityF)

  /** id категории. */
  def userCatIdM = esIdM
  val userCatIdOptM = optional(userCatIdM)

  // TODO Нужен нормальный валидатор телефонов.
  val phoneM = nonEmptyText(minLength = 5, maxLength = 16)

  def martAddressM = addressM

  // Трансформеры для optional-списков.
  def optList2ListF[T] = { optList: Option[List[T]] => optList getOrElse Nil }
  def list2OptListF[T] = { l:List[T] =>  if (l.isEmpty) None else Some(l) }

  /** Маппер form-поля URL в строку URL */
  val urlStrMapper = nonEmptyText(minLength = 8)
    .transform(strTrimF, strIdentityF)
    .verifying("mappers.url.invalid_url", isValidUrl(_))

  /** Маппер form-поля с ссылкой в java.net.URL. */
  val urlMapper = urlStrMapper
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
  val float = nonEmptyText(maxLength = 15)
    .verifying("float.invalid", floatRe.pattern.matcher(_).matches())
    .transform(_.toFloat, {f: Float => f.toString})


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


  def adhocPercentFmt(pc: Float) = TplDataFormatUtil.formatPercentRaw(pc) + "%"

  // Процентные значения
  /** Нестрогий маппер процентов. Крэшится только если слишком много букв. */
  val percentM = {
    text(maxLength = 20)
      .transform[(String, Option[Float])](
        {raw =>
          val raw1 = strTrimSanitizeF(raw)
          raw1 -> parsePercents(raw1)
        },
        { case (raw, opt) if !raw.isEmpty || opt.isEmpty => raw
          case (raw, Some(pc)) => adhocPercentFmt(pc) }
      )
  }

  /** Строгий маппер скидки в процентах. */
  val discountPercentM: Mapping[Float] = {
    percentM
      .verifying("error.required", _._2.isDefined)
      .verifying("error.discount.too.large", { _._2.get <= 200F })
      .verifying("error.discount.too.small", { _._2.get >= -99.999999F })
      .transform[Float](
        _._2.get,
        {pc => adhocPercentFmt(pc) -> Some(pc)}
      )
  }

}


object FormHelpers {

  implicit val myFields = FieldConstructor(lkFieldConstructor.f)

}

