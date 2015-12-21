package models

import java.util.Currency
import io.suggest.common.menum.EnumMaybeWithName
import models.mctx.Context
import play.api.mvc.{RequestHeader, Call}
import _root_.util.PlayLazyMacroLogsImpl
import play.mvc.Http.Request
import play.twirl.api.Html
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.04.14 17:09
 * Description: Моделе-подобное барахло, которые в основном нужно для шаблонов.
 */

object CurrencyCodeOpt extends PlayLazyMacroLogsImpl {
  val CURRENCY_CODE_DFLT = "RUB"
}


trait CurrencyCode {
  import CurrencyCodeOpt.LOGGER._

  def currencyCode: String

  def currency: Currency = {
    try {
      Currency.getInstance(currencyCode)
    } catch {
      case ex: Exception =>
        error("Unsupported/unknown currency code: " + currencyCode + "; Supported are " + Currency.getAvailableCurrencies.toSeq.map(_.getCurrencyCode).mkString(", "), ex)
        throw ex
    }
  }
}

/** Опциональное поле currencyCode, подразумевающее дефолтовую валюту. */
trait CurrencyCodeOpt extends CurrencyCode {
  def currencyCodeOpt : Option[String]
  def currencyCode = currencyCodeOpt getOrElse CurrencyCodeOpt.CURRENCY_CODE_DFLT
}

object CurrencyCodeDflt extends CurrencyCode {
  override def currencyCode = CurrencyCodeOpt.CURRENCY_CODE_DFLT
}



/** Размеры аудиторий. */
object AudienceSizes extends EnumMaybeWithName {

  override type T = Value

  val LessThan20 = Value("lt20")
  val LessThan50 = Value("lt50")
  val Greater50  = Value("gt50")

}


/** Экземпляр запроса помощи через обратную связь в ЛК. */
case class MLkSupportRequest(
  name        : Option[String],
  replyEmail  : String,
  msg         : String,
  phoneOpt    : Option[String] = None
)



/** Enum для задания параметра подсветки текущей ссылки на правой панели личного кабинета узла. */
object NodeRightPanelLinks extends Enumeration {
  type T = Value
  val RPL_NODE, RPL_NODE_EDIT, RPL_USER_EDIT, RPL_ADVERTISERS = Value : T
}

/** Enum для задания параметра подсветки текущей ссылки на правой панели в разделе биллинга узла. */
object BillingRightPanelLinks extends Enumeration {
  type T = Value
  val RPL_BILLING, RPL_TRANSACTIONS, RPL_REQUISITES = Value : T
}

/** Enum для задания параметра подсветки текущей ссылки на левой панели ЛК.*/
object LkLeftPanelLinks extends Enumeration {
  type T = Value
  val LPL_NODE, LPL_ADS, LPL_BILLING, LPL_SUPPORT, LPL_EVENTS  =  Value : T
}


/**
 * Экземпляр хранит вызов к внешнему серверу. Кроме как для индикации этого факта, класс ни для чего
 * больше не используется.
 * @param url Ссылка для вызова.
 * @param method - Обычно "GET", который по умолчанию и есть.
 */
class ExternalCall(
  url     : String,
  method  : String = "GET"
) extends Call(method = method, url = url) {

  override def absoluteURL(secure: Boolean)(implicit request: RequestHeader): String = url
  override def absoluteURL(request: Request): String = url
  override def absoluteURL(request: Request, secure: Boolean): String = url
  override def absoluteURL(secure: Boolean, host: String): String = url

}



/** Интерфейс для возможности задания моделей, умеющих рендер в html. */
trait IRenderable {
  /** Запуск рендера в контексте рендера шаблонов. */
  def render()(implicit ctx: Context): Html
}

