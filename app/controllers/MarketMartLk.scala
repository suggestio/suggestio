package controllers

import util.PlayMacroLogsImpl
import util.acl._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import models._
import MMart.MartId_t
import MShop.ShopId_t
import io.suggest.model.EsModel
import views.html.market.lk.mart._
import play.api.data._, Forms._
import util.FormUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.14 13:54
 * Description: Личный кабинет для sio-маркета. Тут управление торговым центром и магазинами в нём.
 */
object MarketMartLk extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  // Допустимые значения сортировки при выдаче магазинов.
  object ShopSort extends Enumeration {
    val SORT_BY_A_Z   = Value("a-z")
    val SORT_BY_CAT   = Value("cat")
    val SORT_BY_FLOOR = Value("floor")
  }

  import ShopSort._

  /** Маппинг для формы добавления/редактирования торгового центра. */
  val martFormM = Form(mapping(
    "name" -> nonEmptyText(maxLength = 64)
      .transform(strTrimF, strIdentityF),
    "town" -> nonEmptyText(maxLength = 32)
      .transform(strTrimF, strIdentityF),
    "address" -> nonEmptyText(minLength = 10, maxLength = 128)
      .transform(strTrimF, strIdentityF),
    "site_url" -> optional(urlStrMapper)
  )
  // applyF()
  {(name, town, address, siteUrlOpt) =>
    MMart(name=name, town=town, address=address, siteUrl=siteUrlOpt, companyId=null)
  }
  // unapplyF()
  {mmart =>
    import mmart._
    Some((name, town, address, siteUrl))}
  )

  /** Маппинг формы приглашения магазина в систему. */
  val inviteShopFormM = Form(mapping(
    "name" -> nonEmptyText(maxLength = 64)
      .transform(strTrimF, strIdentityF),
    "email"         -> email,
    "mart_floor"    -> martFloorM,
    "mart_section"  -> martSectionM
  )
  // applyF()
  {(name, email, martFloor, martSection) =>
    email -> MShop(name=name, martFloor=Some(martFloor), martSection=Some(martSection), companyId=null)
  }
  // unapplyF()
  {case (_email, mshop) =>
    import mshop._
    Some((name, _email, martFloor.get, martSection.get))
  })


  /**
   * Рендер раздачи страницы с личным кабинетом торгового центра.
   * @param martId id ТЦ
   * @param sortByRaw Сортировка магазинов по указанному полю. Если не задано, то порядок не определён.
   * @param isReversed Если true, то будет сортировка в обратном порядке. Иначе в прямом.
   */
  def martShow(martId: MartId_t, sortByRaw: Option[String], isReversed: Boolean) = IsMartAdmin(martId).async { implicit request =>
    val mmartOptFut = MMart.getById(martId)
    val sortBy = sortByRaw flatMap {
      _sortByRaw => Option(handleShopsSortBy(_sortByRaw))
    }
    val shopsFut = MShop.findByMartId(martId, sortBy, isReversed)
    mmartOptFut flatMap {
      case Some(mmart) =>
        shopsFut map { shops =>
          Ok(martShowTpl(mmart, shops))
        }

      case None => martNotFound(martId)
    }
  }

  /**
   * Рендер страницы с формой редактирования ТЦ в личном кабинете.
   * @param martId id ТЦ.
   */
  def martEditForm(martId: MartId_t) = IsMartAdmin(martId).async { implicit request =>
    MMart.getById(martId) map {
      case Some(mmart) =>
        val formFilled = martFormM.fill(mmart)
        Ok(martEditFormTpl(mmart, formFilled))

      case None => martNotFound(martId)
    }
  }

  /**
   * Сабмит формы редактирования ТЦ.
   * @param martId id ТЦ.
   */
  def martEditFormSubmit(martId: MartId_t) = IsMartAdmin(martId).async { implicit request =>
    MMart.getById(martId) flatMap {
      case Some(mmart) =>
        martFormM.bindFromRequest().fold(
          {formWithErrors =>
            debug(s"martEditFormSubmit($martId): Failed to bind form: " + formWithErrors.errors)
            NotAcceptable(martEditFormTpl(mmart, formWithErrors))
              .flashing("error" -> "Ошибка заполнения формы.")
          },
          {mmart2 =>
            mmart.name = mmart2.name
            mmart.town = mmart2.town
            mmart.address = mmart2.address
            mmart.siteUrl = mmart2.siteUrl
            mmart.save.map { _ =>
              Redirect(routes.MarketMartLk.martShow(martId))
                .flashing("success" -> "Изменения сохранены.")
            }
          }
        )

      case None => martNotFound(martId)
    }
  }


  /**
   * Рендер страницы с формой инвайта магазина.
   * @param martId id ТЦ.
   */
  def inviteShopForm(martId: MartId_t) = IsMartAdmin(martId).async { implicit request =>
    MMart.getById(martId) map {
      case Some(mmart) =>
        Ok(shop.shopInviteFormTpl(mmart, inviteShopFormM))

      case None => martNotFound(martId)
    }
  }

  /**
   * Сабмит формы инвайта магазина.
   * @param martId id ТЦ.
   */
  def inviteShopFormSubmit(martId: MartId_t) = IsMartAdmin(martId).async { implicit request =>
    inviteShopFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"inviteShopFormSubmit($martId): Bind failed: " + formWithErrors.errors)
        MMart.getById(martId) map {
          case Some(mmart) => NotAcceptable(shop.shopInviteFormTpl(mmart, formWithErrors))
          case None => martNotFound(martId)
        }
      },
      {case (_email, mshop) =>
        mshop.martId = Some(martId)
        MCompany(mshop.name).save.flatMap { companyId =>
          mshop.companyId = companyId
          mshop.save.map { shopId =>
            // TODO Отсылать почту на почтовый адрес
            warn("TODO Not yet implemented: send email to " + _email)
            // TODO Редиректить в личный кабинет магазина
            Redirect(routes.MarketMartLk.martShow(martId))
              .flashing("success" -> s"Добавлен магазин '${mshop.name}'.")
          }
        }
      }
    )
  }

  /**
   * Сабмит формы удаления магазина из торгового центра.
   * @param martId id ТЦ.
   * @param shopId id Магазина.
   */
  def martShopDeleteSubmit(martId: MartId_t, shopId: ShopId_t) = IsMartAdmin(martId).async { implicit request =>
    MShop.getMartIdFor(shopId) flatMap {
      case Some(shopMartId) if shopMartId == martId =>
        MShop.deleteById(shopId) map { _ =>
          Redirect(routes.MarketMartLk.martShow(martId))
            .flashing("success" -> "Магазин удалён.")
        }

      case None => NotFound("Shop not found or unrelated.")
    }
  }


  private def martNotFound(martId: MartId_t) = NotFound("mart not found: " + martId)  // TODO Нужно дергать 404-шаблон.

  private def handleShopsSortBy(sortRaw: String): String = {
    if (SORT_BY_A_Z.toString equalsIgnoreCase sortRaw) {
      EsModel.NAME_ESFN
    } else if (SORT_BY_CAT.toString equalsIgnoreCase sortRaw) {
      ???
    } else if (SORT_BY_FLOOR.toString equalsIgnoreCase sortRaw) {
      EsModel.MART_FLOOR_ESFN
    } else {
      null
    }
  }

}
