package controllers

import util.PlayMacroLogsImpl
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import play.api.data._, Forms._
import util.FormUtil._
import util.acl._
import models._, MShop.ShopId_t, MMart.MartId_t
import views.html.market.lk.shop._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.03.14 13:34
 * Description: Контроллер личного кабинета для арендатора, т.е. с точки зрения конкретного магазина.
 */
object MarketShopLk extends SioController with PlayMacroLogsImpl {

  import LOGGER._

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


  /**
   * Рендер страницы магазина (с точки зрения арендатора: владельца магазина).
   * @param shopId id магазина.
   */
  def showShop(shopId: ShopId_t) = IsMartShopAdmin(shopId).async { implicit request =>
    val adsFut = MMartAd.findForShop(shopId)
    MShop.getById(shopId) flatMap {
      case Some(mshop) =>
        // TODO Если магазин удалён из ТЦ, то это как должно выражаться?
        val martId = mshop.martId.get
        MMart.getById(martId).flatMap {
          case Some(mmart) =>
            adsFut.map { ads =>
              Ok(shopShowTpl(mmart, mshop, ads))
            }

          case None => martNotFound(martId)
        }

      case None => shopNotFound(shopId)
    }
  }

  /**
   * Страница с формой редактирования магазина. Арендатору не доступны некоторые поля.
   * @param shopId id магазина.
   */
  def editShopForm(shopId: ShopId_t) = IsMartShopAdmin(shopId).async { implicit request =>
    MShop.getById(shopId) flatMap {
      case Some(mshop) =>
        // TODO Если магазин удалён из ТЦ, то это как должно выражаться?
        val martId = mshop.martId.get
        MMart.getById(martId).map {
          case Some(mmart) =>
            val formBinded = shopFormM.fill(mshop)
            Ok(shopEditFormTpl(mmart, mshop, formBinded))

          case None => martNotFound(martId)
        }

      case None => shopNotFound(shopId)
    }
  }

  /**
   * Сабмит формы редактирования магазина арендатором.
   * @param shopId id магазина.
   */
  def editShopFormSubmit(shopId: ShopId_t) = IsMartShopAdmin(shopId).async { implicit request =>
    MShop.getById(shopId) flatMap {
      case Some(mshop) =>
        limitedShopFormM.bindFromRequest().fold(
          {formWithErrors =>
            debug(s"editShopFormSubmit($shopId): Bind failed: " + formWithErrors.errors)
            val martId = mshop.martId.get
            MMart.getById(martId).map {
              case Some(mmart) => NotAcceptable(shopEditFormTpl(mmart, mshop, formWithErrors))
              case None        => martNotFound(martId)
            }
          },
          {case (name, description) =>
            mshop.name = name
            mshop.description = description
            mshop.save.map { _ =>
              Redirect(routes.MarketShopLk.showShop(shopId))
                .flashing("success" -> "Изменения сохранены.")
            }
          }
        )

      case None => shopNotFound(shopId)
    }
  }


  private def martNotFound(martId: MartId_t) = NotFound("mart not found: " + martId)  // TODO Нужно дергать 404-шаблон.
  private def shopNotFound(shopId: ShopId_t) = NotFound("Shop not found: " + shopId)  // TODO

}
