package models

import models.mctx.Context
import play.twirl.api.Html

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.04.14 17:09
 * Description: Моделе-подобное барахло, которые в основном нужно для шаблонов.
 */


/** Enum для задания параметра подсветки текущей ссылки на правой панели личного кабинета узла. */
object NodeRightPanelLinks extends Enumeration {
  type T = Value
  val RPL_NODE, RPL_NODE_EDIT, RPL_USER_EDIT, RPL_ADN_MAP, RPL_NODES = Value : T
}

/** Enum для задания параметра подсветки текущей ссылки на правой панели в разделе биллинга узла. */
object BillingRightPanelLinks extends Enumeration {
  type T = Value
  val RPL_BILLING, RPL_CART, RPL_ORDERS = Value : T
}

/** Enum для задания параметра подсветки текущей ссылки на левой панели ЛК.*/
object LkLeftPanelLinks extends Enumeration {
  type T = Value
  val LPL_NODE, LPL_ADS, LPL_BILLING, LPL_SUPPORT =  Value : T
}


/** Интерфейс для возможности задания моделей, умеющих рендер в html. */
trait IRenderable {
  /** Запуск рендера в контексте рендера шаблонов. */
  def render()(implicit ctx: Context): Html
}

