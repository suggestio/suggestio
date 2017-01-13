package models

import models.mctx.Context
import play.api.mvc.{RequestHeader, Call}
import play.mvc.Http.Request
import play.twirl.api.Html

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.04.14 17:09
 * Description: Моделе-подобное барахло, которые в основном нужно для шаблонов.
 */

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
  val RPL_NODE, RPL_NODE_EDIT, RPL_USER_EDIT, RPL_ADN_MAP = Value : T
}

/** Enum для задания параметра подсветки текущей ссылки на правой панели в разделе биллинга узла. */
object BillingRightPanelLinks extends Enumeration {
  type T = Value
  val RPL_BILLING, RPL_TRANSACTIONS, RPL_CART = Value : T
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
)
  extends Call(method = method, url = url)
{

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

