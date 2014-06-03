package controllers

import util.PlayMacroLogsImpl
import util.acl._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import views.html.market.lk.mart._
import play.api.data._, Forms._
import util.FormUtil._
import com.typesafe.plugin.{use, MailerPlugin}
import play.api.Play.current
import util.acl.IsMartAdminShop
import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.14 13:54
 * Description: Личный кабинет для sio-маркета. Тут управление торговым центром и магазинами в нём.
 */
object MarketMartLk extends SioController with PlayMacroLogsImpl with BruteForceProtect with ShopMartCompat {

  import LOGGER._

  private val shopMetaM = mapping(
    "name"    -> shopNameM,
    "floor"   -> floorM,
    "section" -> sectionM
  )
  {(name, floor, section) =>
    AdnMMetadata(name, floor = Some(floor), section = Some(section))
  }
  {lei =>
    import lei._
    Some( (name, floor.getOrElse(""), section.getOrElse("")) )
  }

  /** Маппинг формы приглашения магазина в систему. */
  private val inviteShopFormM = Form(tuple(
    "email" -> email,
    "meta" -> shopMetaM
  ))


  /** Форма на которой нельзя менять логотип, но можно настраивать разные поля.
    * Подходит для редактирования из ТЦ-аккаунта */
  private val shopEditFormM = Form(
    MarketShopLk.shopMetaFullKM
  )


  /**
   * Рендер страницы с формой инвайта магазина.
   * @param martId id ТЦ.
   */
  def inviteShopForm(martId: String) = IsMartAdmin(martId).apply { implicit request =>
    Ok(shop.shopInviteFormTpl(request.mmart, inviteShopFormM))
  }

  /**
   * Сабмит формы инвайта магазина.
   * @param martId id ТЦ.
   */
  def inviteShopFormSubmit(martId: String) = IsMartAdmin(martId).async { implicit request =>
    import request.mmart
    inviteShopFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"inviteShopFormSubmit($martId): Bind failed: ${formatFormErrors(formWithErrors)}")
        NotAcceptable(shop.shopInviteFormTpl(mmart, formWithErrors))
      },
      {case (_email, meta) =>
        MCompany(meta.name).save.flatMap { companyId =>
          // Собираем дефолтовый магазин как будущий узел рекламной сети.
          val mshop = MAdnNode(
            companyId = companyId,
            personIds = Set.empty,
            adn = {
              val mi = AdNetMemberTypes.SHOP.getAdnInfoDflt
              mi.supId = Some(martId)
              mi.isEnabled = false
              mi.disableReason = Some("First run/inactive")
              mi
            },
            meta = meta
          )
          mshop.handleMeAddedAsChildFor(mmart)
          mshop.save.flatMap { shopId =>
            mshop.id = Some(shopId)
            // Добавить магазин как источник рекламного контента для ТЦ.
            mmart.handleChildNodeAddedToMe(mshop)
            val mmartSaveFut = mmart.save
            // Это зачем-то надо:
            mshop.id = Some(shopId)
            // Магазин создан. Отправить инвайт будущему владельцу по почте.
            val eAct = EmailActivation(email = _email, key = shopId)
            // Дождаться, пока всё сохранится...
            for {
              eaId <- eAct.save
              _    <- mmartSaveFut
            } yield {
              eAct.id = Some(eaId)
              // Пора отправлять письмо юзеру с ссылкой для активации.
              trace(s"inviteShopFormSubmit($martId): shopId=$shopId companyId=$companyId eAct=$eAct :: Sending message to ${_email} ...")
              val mail = use[MailerPlugin].email
              mail.setSubject("Suggest.io | Подтверждение регистрации")
              mail.setFrom("no-reply@suggest.io")
              mail.setRecipient(_email)
              val ctx = implicitly[Context]   // нано-оптимизация: один контекст для обоих шаблонов.
              mail.send(
                bodyHtml = shop.emailShopInviteTpl(mmart, mshop=mshop, eAct)(ctx),
                bodyText = views.txt.market.lk.mart.shop.emailShopInviteTpl(mmart, mshop=mshop, eAct)(ctx)
              )
              // Собственно, результат работы.
              Redirect(routes.MarketLkAdn.showAdnNode(martId))
                .flashing("success" -> s"Добавлен магазин: '${mshop.meta.name}'.")
            }
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
  def editShopForm(shopId: String) = IsMartAdminShop(shopId).async { implicit request =>
    import request.{mmart, mshop}
    MarketShopLk.fillFullForm(mshop) map { formBinded =>
      Ok(shop.shopEditFormTpl(mmart, mshop, formBinded))
    }
  }


  /**
   * Сабмит формы редактирования магазина-арендатора.
   * @param shopId id редактируемого магазина.
   */
  def editShopFormSubmit(shopId: String) = IsMartAdminShop(shopId).async { implicit request =>
    import request.mshop
    shopEditFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"editShopFormSubmit($shopId): Form bind failed: ${formatFormErrors(formWithErrors)}")
        MarketShopLk.fillFullForm(mshop) map { formWithErrors2 =>
          val fwe3 = formWithErrors2.bindFromRequest()
          NotAcceptable(shop.shopEditFormTpl(request.mmart, mshop, fwe3))
        }
      },
      {meta =>
        // Пора накатить изменения на текущий магазин и сохранить
        mshop.meta.name = meta.name
        mshop.meta.description = meta.description
        mshop.meta.floor = meta.floor
        mshop.meta.section = meta.section
        mshop.meta.color = meta.color
        mshop.save.map { _ =>
          Redirect(routes.MarketLkAdn.showSlave(shopId))
            .flashing("success" -> "Изменения сохранены.")
        }
      }
    )

  }

}
