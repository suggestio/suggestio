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
import MarketShopLk.shopFormM

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

    def handleShopsSortBy(sortRaw: String): Option[String] = {
      if (SORT_BY_A_Z.toString equalsIgnoreCase sortRaw) {
        Some(EsModel.NAME_ESFN)
      } else if (SORT_BY_CAT.toString equalsIgnoreCase sortRaw) {
        ???
      } else if (SORT_BY_FLOOR.toString equalsIgnoreCase sortRaw) {
        Some(EsModel.MART_FLOOR_ESFN)
      } else {
        None
      }
    }
  }

  import ShopSort._

  /** Маппинг для формы добавления/редактирования торгового центра. */
  val martFormM = Form(mapping(
    "name"      -> martNameM,
    "town"      -> townM,
    "address"   -> martAddressM,
    "site_url"  -> optional(urlStrMapper),
    "phone"     -> optional(phoneM)
  )
  // applyF()
  {(name, town, address, siteUrlOpt, phoneOpt) =>
    MMart(name=name, town=town, address=address, siteUrl=siteUrlOpt, companyId=null, phone=phoneOpt)
  }
  // unapplyF()
  {mmart =>
    import mmart._
    Some((name, town, address, siteUrl, phone))}
  )

  /** Маппинг формы приглашения магазина в систему. */
  val inviteShopFormM = Form(mapping(
    "name"          -> shopNameM,
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
    val sortBy = sortByRaw flatMap handleShopsSortBy
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
            mmart.phone = mmart2.phone
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
   * Рендер страницы с формой редактирования магазина-арендатора.
   * Владельцу ТЦ доступны различные опции формы, недоступные для редактирования арендатору, поэтому для биндинга
   * используется именно расширенная форма.
   * @param shopId id магазина.
   */
  def editShopForm(shopId: ShopId_t) = IsMartAdminShop(shopId).async { implicit request =>
    val martId = request.martId
    val mmartOptFut = MMart.getById(martId)
    MShop.getById(shopId) flatMap {
      case Some(mshop) =>
        mmartOptFut.map {
          case Some(mmart) =>
            val formBinded = shopFormM.fill(mshop)
            Ok(shop.shopEditFormTpl(mmart, mshop, formBinded))

          case None => martNotFound(martId)
        }

      case None => shopNotFound(shopId)
    }
  }

  /**
   * Сабмит формы редактирования магазина-арендатора.
   * @param shopId id редактируемого магазина.
   */
  def editShopFormSubmit(shopId: ShopId_t) = IsMartAdminShop(shopId).async { implicit request =>
    MShop.getById(shopId) flatMap {
      case Some(mshop) =>
        shopFormM.bindFromRequest().fold(
          {formWithErrors =>
            debug(s"editShopFormSubmit($shopId): Form bind failed: " + formWithErrors.errors)
            val martId = request.martId
            MMart.getById(martId) map {
              case Some(mmart) => NotAcceptable(shop.shopEditFormTpl(mmart, mshop, formWithErrors))
              case None => martNotFound(martId)
            }
          },
          {mshop2 =>
            // Пора накатить изменения на текущий магазин и сохранить
            mshop.name = mshop2.name
            mshop.description = mshop2.description
            mshop.martFloor = mshop2.martFloor
            mshop.martSection = mshop2.martSection
            mshop.save.map { _ =>
              Redirect(routes.MarketMartLk.showShop(shopId))
                .flashing("success" -> "Изменения сохранены.")
            }
          }
        )

      case None => shopNotFound(shopId)
    }
  }

  /**
   * Отобразить страницу по магазину.
   * @param shopId id магазина.
   */
  def showShop(shopId: ShopId_t) = IsMartAdminShop(shopId).async { implicit request =>
    val martId = request.martId
    val mmartOptFut = MMart.getById(martId)
    MShop.getById(shopId) flatMap {
      case Some(mshop) =>
        mmartOptFut.map {
          case Some(mmart) => Ok(shop.shopShowTpl(mmart, mshop))
          case None => martNotFound(martId)
        }

      case None => shopNotFound(shopId)
    }
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

  /**
   * Страница со списком товаров магазина.
   * @param shopId id магазина.
   */
  def showShopOffers(shopId: ShopId_t) = IsMartAdminShop(shopId).async { implicit request =>
    val martId = request.martId
    val mmartOptFut = MMart.getById(martId)
    val moffersFut = MShopPromoOffer.getAllForShop(shopId)
    MShop.getById(shopId) flatMap {
      case Some(mshop) =>
        mmartOptFut flatMap {
          case Some(mmart) =>
            moffersFut.map { moffers =>
              Ok(shop.shopOffersTpl(mmart, mshop, moffers))
            }

          case None => martNotFound(martId)
        }

      case None => shopNotFound(shopId)
    }
  }

  private def martNotFound(martId: MartId_t) = NotFound("mart not found: " + martId)  // TODO Нужно дергать 404-шаблон.
  private def shopNotFound(shopId: ShopId_t) = NotFound("Shop not found: " + shopId)  // TODO

}
