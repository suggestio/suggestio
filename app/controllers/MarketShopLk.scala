package controllers

import util.PlayMacroLogsImpl
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import play.api.data._, Forms._
import util.FormUtil._
import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.03.14 13:34
 * Description: Контроллер для shop-части личного кабинета в маркете.
 */
object MarketShopLk extends SioController with PlayMacroLogsImpl {

  /** Форма добавления/редактирования магазина. */
  val shopFormM = Form(mapping(
    "name"         -> shopNameM,
    "description"  -> publishedTextOptM,
    "mart_floor"   -> optional(martFloorM),
    "mart_section" -> optional(martSectionM)
  )
  // apply()
  {(name, description, martFloor, martSection) =>
    MShop(name=name, companyId=null, description=description, martFloor=martFloor, martSection=martSection)
  }
  // unapply()
  {mshop =>
    import mshop._
    Some((name, description, martFloor, martSection))
  })

  /** Ограниченный маппинг магазина. Используется редактировании оного для имитации неизменяемых полей на форме.
    * Некоторые поля не доступны для редактирования владельцу магазина, и эта форма как раз для него. */
  val limitedShopFormM = Form(tuple(
    "name"         -> shopNameM,
    "description"  -> publishedTextOptM
  ))


}
