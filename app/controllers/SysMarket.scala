package controllers

import io.suggest.util.MacroLogsImpl
import util.acl.{AbstractRequestWithPwOpt, IsSuperuserAdnNode, IsSuperuser}
import models._
import views.html.sys1.market._
import views.html.market.lk
import play.api.data._, Forms._
import util.FormUtil._
import io.suggest.ym.model.UsernamePw
import MCompany.CompanyId_t
import play.api.libs.concurrent.Execution.Implicits._
import util.SiowebEsUtil.client
import util.Context
import com.typesafe.plugin.{use, MailerPlugin}
import play.api.Play.current
import io.suggest.ym.ad.ShowLevelsUtil
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.02.14 17:21
 * Description: Тут управление компаниями, торговыми центрами и магазинами.
 */
object SysMarket extends SioController with MacroLogsImpl with ShopMartCompat {

  import LOGGER._

  /** Маппинг для формы добавления/редактирования компании. */
  val companyFormM = Form(
    "name" -> companyNameM
  )

  /** Индексная страница продажной части. Тут ссылки на дальнейшие страницы. */
  def index = IsSuperuser { implicit request =>
    Ok(marketIndexTpl())
  }

  /** Отрендерить sio-админу список всех компаний, зарегистрированных в системе. */
  def companiesList = IsSuperuser.async { implicit request =>
    MCompany.getAll().map { allCompanies =>
      val render = company.companiesListTpl(allCompanies)
      Ok(render)
    }
  }

  /** Отрендерить страницу с формой добавления новой компании. */
  def companyAddForm = IsSuperuser { implicit request =>
    Ok(company.companyAddFormTpl(companyFormM))
  }

  /** Самбит формы добавления новой компании. */
  def companyAddFormSubmit = IsSuperuser.async { implicit request =>
    companyFormM.bindFromRequest.fold(
      {formWithErrors =>
        NotAcceptable(company.companyAddFormTpl(formWithErrors))
      },
      {name =>
        MCompany(name).save.map { companyId =>
          Redirect(routes.SysMarket.companyShow(companyId))
        }
      }
    )
  }

  /** Отобразить информацию по указанной компании.
    * @param companyId Числовой id компании.
    */
  def companyShow(companyId: CompanyId_t) = IsSuperuser.async { implicit request =>
    val companyAdnmsFut = MAdnNode.findByCompanyId(companyId)
    MCompany.getById(companyId) flatMap {
      case Some(mc) =>
        for {
          adnms <- companyAdnmsFut
        } yield {
          Ok(company.companyShowTpl(mc, adnms))
        }

      case None => companyNotFound(companyId)
    }
  }


  /** Отрендерить страницу с формой редактирования компании. */
  def companyEditForm(companyId: CompanyId_t) = IsSuperuser.async { implicit request =>
    MCompany.getById(companyId) map {
      case Some(mc)  =>
        val form = companyFormM.fill(mc.name)
        Ok(company.companyEditFormTpl(mc, form))

      case None => companyNotFound(companyId)
    }
  }

  /** Сабмит формы редактирования компании. */
  def companyEditFormSubmit(companyId: CompanyId_t) = IsSuperuser.async { implicit request =>
    MCompany.getById(companyId) flatMap {
      case Some(mc) =>
        companyFormM.bindFromRequest.fold(
          {formWithErrors =>
            NotAcceptable(company.companyEditFormTpl(mc, formWithErrors))
          },
          {name =>
            mc.name = name
            mc.save map { _ =>
              Redirect(routes.SysMarket.companyShow(companyId))
            }
          }
        )

      case None => companyNotFound(companyId)
    }
  }

  /** Админ приказал удалить указанную компанию. */
  def companyDeleteSubmit(companyId: CompanyId_t) = IsSuperuser.async { implicit request =>
    MCompany.deleteById(companyId) map {
      case true =>
        Redirect(routes.SysMarket.companiesList())
          .flashing("success" -> s"Company $companyId deleted.")

      case false => companyNotFound(companyId)
    }
  }


  /** Реакция на ошибку обращения к несуществующей компании. Эта логика расшарена между несколькими экшенами. */
  private def companyNotFound(companyId: CompanyId_t) = NotFound("Company not found: " + companyId)


  /* Унифицированные узлы ADN */
  import views.html.sys1.market.adn._

  /** Страница с унифицированным списком узлов рекламной сети в алфавитном порядке с делёжкой по memberType.  */
  def adnNodesList(anmtRaw: Option[String]) = IsSuperuser.async { implicit request =>
    val companiesFut = MCompany.getAll(maxResults = 1000).map {
      _.map {c => c.id.get -> c }.toMap
    }
    val adnNodesFut = anmtRaw match {
      case Some(_anmtRaw) =>
        val anmt = AdNetMemberTypes.withName(_anmtRaw)
        MAdnNode.findAllByType(anmt, maxResult = 1000)
      case None =>
        MAdnNode.getAll(maxResults = 1000)
    }
    for {
      adnNodes <- adnNodesFut
      companies <- companiesFut
    } yield {
      Ok(adnNodesListTpl(adnNodes, Some(companies)))
    }
  }

  /** Унифицированая страница отображения узла рекламной сети. */
  def showAdnNode(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    import request.adnNode
    val slavesFut = MAdnNode.findBySupId(adnId)
    val companyOptFut = MCompany.getById(adnNode.companyId)
    for {
      slaves <- slavesFut
      companyOpt <- companyOptFut
    } yield {
      Ok(adnNodeShowTpl(adnNode, slaves, companyOpt))
    }
  }

  /** Безвозвратное удаление узла рекламной сети. */
  def deleteAdnNodeSubmit(adnId: String) = IsSuperuser.async { implicit request =>
    MAdnNode.deleteById(adnId) map {
      case true =>
        Redirect(routes.SysMarket.adnNodesList())
          .flashing("success" -> "Узел ADN удалён.")
      case false => NotFound("ADN node not found: " + adnId)
    }
  }


  /** Форма для маппинг метаданных произвольного узла ADN. */
  private val adnNodeMetaM = mapping(
    "name"      -> nameM,
    "descr"     -> publishedTextOptM,
    "town"      -> townOptM,
    "address"   -> addressOptM,
    "phone"     -> phoneOptM,
    "floor"     -> floorOptM,
    "section"   -> sectionOptM,
    "siteUrl"   -> urlStrOptM,
    "color"     -> colorOptM
  )
  {(name, descr, town, address, phone, floor, section, siteUrl, color) =>
    AdnMMetadata(
      name    = name,
      description = descr,
      town    = town,
      address = address,
      phone   = phone,
      floor   = floor,
      section = section,
      siteUrl = siteUrl,
      color   = color
    )
  }
  {meta =>
    import meta._
    Some((name, description, town, address, phone, floor, section, siteUrl, color))
  }

  /** Маппинг для adn-полей формы adn-узла. */
  private val adnMemberM: Mapping[AdNetMemberInfo] = mapping(
    "memberType" -> adnMemberTypeM,
    "isEnabled"  -> boolean
  )
  {(mt, isEnabled) =>
    val result = mt.getAdnInfoDflt
    result.isEnabled = isEnabled
    result
  }
  {anmi =>
    Some((anmi.memberType, anmi.isEnabled))
  }


  /** Маппинг для формы добавления/редактирования торгового центра. */
  private val adnNodeFormM = Form(mapping(
    "companyId" -> esIdM,
    "adn"       -> adnMemberM,
    "meta"      -> adnNodeMetaM,
    "maxAds"    -> default(
      number(min = 0, max = 30),
      ShowLevelsUtil.MART_LVL_IN_START_PAGE_DFLT
    )
  )
  // apply()
  {(companyId, anmi, meta, maxAds) =>
    anmi.showLevelsInfo.setMaxOutAtLevel(anmi.memberType.slDflt, maxAds)
    MAdnNode(
      meta = meta,
      companyId = companyId,
      adn = anmi,
      personIds = Set.empty
    )
  }
  // unapply()
  {adnNode =>
    import adnNode._
    val maxAds = adn.showLevelsInfo.maxOutAtLevel(adnNode.adn.memberType.slDflt)
    Some((companyId, adn, meta, maxAds))
  })

  private def maybeSupOpt(supIdOpt: Option[String]): Future[Option[MAdnNode]] = {
    supIdOpt match {
      case Some(supId) => MAdnNodeCache.getByIdCached(supId)
      case None => Future successful None
    }
  }
  private def createAdnNodeRender(supOptFut: Future[Option[MAdnNode]], supIdOpt: Option[String])(implicit request: AbstractRequestWithPwOpt[_]) = {
    val companiesFut = MCompany.getAll(maxResults = 100)
    for {
      supOpt    <- supOptFut
      companies <- companiesFut
    } yield {
      createAdnNodeFormTpl(supOpt, adnNodeFormM, companies)
    }
  }

  /** Страница с формой создания нового узла. */
  def createAdnNode(supIdOpt: Option[String]) = IsSuperuser.async { implicit request =>
    createAdnNodeRender(maybeSupOpt(supIdOpt), supIdOpt) map { Ok(_) }
  }

  /** Сабмит формы создания нового узла. */
  def createAdnNodeSubmit(supIdOpt: Option[String]) = IsSuperuser.async { implicit request =>
    val supOptFut = maybeSupOpt(supIdOpt)
    adnNodeFormM.bindFromRequest().fold(
      {formWithErrors =>
        val renderFut = createAdnNodeRender(supOptFut, supIdOpt)
        debug(s"createAdnNodeSubmit(supId=$supIdOpt): Failed to bind form: ${formatFormErrors(formWithErrors)}")
        renderFut map {
          NotAcceptable(_)
        }
      },
      {adnNode =>
        adnNode.adn.supId = supIdOpt
        supOptFut flatMap { supOpt =>
          if (supIdOpt.isDefined) {
            adnNode.handleMeAddedAsChildFor(supOpt.get)
          }
          for {
            adnId <- adnNode.save
          } yield {
            adnNode.id = Some(adnId)
            if (supIdOpt.isDefined) {
              val sup = supOpt.get
              if (sup.handleChildNodeAddedToMe(adnNode)) {
                sup.save
              }
            }
            Redirect(routes.SysMarket.showAdnNode(adnId))
              .flashing("succes" -> s"Создан узел сети: $adnId")
          }
        }
      }
    )
  }

  /** Страница с формой редактирования узла ADN. */
  def editAdnNode(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    import request.adnNode
    val companiesFut = MCompany.getAll(maxResults = 1000)
    val formFilled = adnNodeFormM.fill(adnNode)
    companiesFut map { companies =>
      Ok(editAdnNodeFormTpl(adnNode, formFilled, companies))
    }
  }
  /** Самбит формы редактирования узла. */
  def editAdnNodeSubmit(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    import request.adnNode
    adnNodeFormM.bindFromRequest().fold(
      {formWithErrors =>
        val companiesFut = MCompany.getAll(maxResults = 1000)
        debug(s"editAdnNodeSubmit($adnId): Failed to bind form: ${formatFormErrors(formWithErrors)}")
        companiesFut map { companies =>
          NotAcceptable(editAdnNodeFormTpl(adnNode, formWithErrors, companies))
        }
      },
      {adnNode2 =>
        adnNode.meta = adnNode2.meta
        adnNode.adn.memberType = adnNode2.adn.memberType
        adnNode.companyId = adnNode2.companyId
        val sl = adnNode2.adn.memberType.slDflt
        val maxAds = adnNode2.adn.showLevelsInfo.maxOutAtLevel(sl)
        adnNode.adn.showLevelsInfo.setMaxOutAtLevel(sl, maxAds)
        adnNode.adn.isEnabled = adnNode2.adn.isEnabled
        adnNode.save.map { _ =>
          Redirect(routes.SysMarket.showAdnNode(adnId))
            .flashing("success" -> "Изменения сохранены")
        }
      }
    )
  }


  /* Торговые центры и площади. */

  private def martNotFound(martId: String) = NotFound("Mart not found: " + martId)


  // Инвайты на управление ТЦ

  val martInviteFormM = Form(
    "email" -> email
  )

  /** Рендер страницы с формой инвайта (передачи прав на управление ТЦ). */
  def martInviteForm(martId: String) = IsSuperuser.async { implicit request =>
    val eActsFut = EmailActivation.findByKey(martId)
    getMartById(martId) flatMap {
      case Some(mmart) =>
        eActsFut map { eActs =>
          Ok(mart.martInviteFormTpl(mmart, martInviteFormM, eActs))
        }
      case None => martNotFound(martId)
    }
  }

  /** Сабмит формы создания инвайта на управление ТЦ. */
  def martInviteFormSubmit(martId: String) = IsSuperuser.async { implicit request =>
    getMartById(martId) flatMap {
      case Some(mmart) =>
        martInviteFormM.bindFromRequest().fold(
          {formWithErrors =>
            debug(s"martInviteFormSubmit($martId): Failed to bind form: ${formWithErrors.errors}")
            EmailActivation.findByKey(martId) map { eActs =>
              NotAcceptable(mart.martInviteFormTpl(mmart, formWithErrors, eActs))
            }
          },
          {email1 =>
            val eAct = EmailActivation(email=email1, key = martId)
            eAct.save.map { eActId =>
              eAct.id = Some(eActId)
              // Собираем и отправляем письмо адресату
              val mail = use[MailerPlugin].email
              mail.setSubject("Suggest.io | Ваш торговый центр")
              mail.setFrom("no-reply@suggest.io")
              mail.setRecipient(email1)
              val ctx = implicitly[Context]   // нано-оптимизация: один контекст для обоих шаблонов.
              mail.send(
                bodyText = views.txt.market.lk.mart.invite.emailMartInviteTpl(mmart, eAct)(ctx),
                bodyHtml = views.html.market.lk.mart.invite.emailMartInviteTpl(mmart, eAct)(ctx)
              )
              // Письмо отправлено, вернуть админа назад в магазин
              Redirect(routes.SysMarket.showAdnNode(martId))
                .flashing("success" -> ("Письмо с приглашением отправлено на " + email1))
            }
          }
        )

      case None => martNotFound(martId)
    }
  }


  /* Магазины (арендаторы ТЦ). */

  /** Рендер ошибки, если магазин не найден в базе. */
  private def shopNotFound(shopId: String) = NotFound("Shop not found: " + shopId)


  /* Ссылки на прайс-листы магазинов, а именно их изменение. */

  /** Маппинг для формы добавления/редактирования ссылок на прайс-листы. */
  private val splFormM = Form(mapping(
    "url"       -> urlStrM,
    "username"  -> optional(text(maxLength = 64)),
    "password"  -> optional(text(maxLength = 64))
  )
  // apply()
  {(url, usernameOpt, passwordOpt) =>
    val authInfo = usernameOpt.map { username =>
      // TODO Убрать второй UserNamePw и использовать датум?
      UsernamePw(
        username,
        password = passwordOpt.getOrElse("")
      )
    }
    url -> authInfo
  }
  // unapply()
  {case (url, authInfoOpt) =>
    Some((url, authInfoOpt.map(_.username), authInfoOpt.map(_.password)))
  })


  /** Рендер формы добавления ссылки на прайс-лист к магазину. */
  def splAddForm(shopId: String) = IsSuperuser.async { implicit request =>
    getShopById(shopId) map {
      case Some(mshop) =>
        Ok(shop.pricelist.splAddFormTpl(mshop, splFormM))

      case None => shopNotFound(shopId)
    }
  }

  /** Сабмит формы добавления прайс-листа. */
  def splAddFormSubmit(shopId: String) = IsSuperuser.async { implicit request =>
    splFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"splAddFormSubmit($shopId): form bind failed: " + formatFormErrors(formWithErrors))
        getShopById(shopId) map {
          case Some(mshop) => NotAcceptable(shop.pricelist.splAddFormTpl(mshop, formWithErrors))
          case None => shopNotFound(shopId)
        }
      },
      {case (url, authInfo) =>
        MShopPriceList(shopId=shopId, url=url, authInfo=authInfo).save map { mspl =>
          Redirect(routes.SysMarket.showAdnNode(shopId))
           .flashing("success" -> "Pricelist added.")
        }
      }
    )
  }

  /** Удалить ранее созданный прайс лист по его id. */
  def splDeleteSubmit(splId: String) = IsSuperuser.async { implicit request =>
    MShopPriceList.getById(splId) map {
      case Some(mspl) =>
        mspl.delete onFailure {
          case ex => error("Unable to delete MSPL id=" + splId, ex)
        }
        Redirect(routes.SysMarket.showAdnNode(mspl.shopId))

      case None => NotFound("No such shop pricelist with id = " + splId)
    }
  }


  /** Отображение категорий яндекс-маркета */
  def showYmCats = IsSuperuser.async { implicit request =>
    MYmCategory.getAllTree.map { cats =>
      Ok(cat.ymCatsTpl(cats))
    }
  }

  /** Полный сброс дерева категорий YM. */
  def resetYmCatsSubmit = IsSuperuser.async { implicit request =>
    // TODO WARNING DANGER ACHTUNG Эту функцию надо выпилить после запуска.
    warn("Resetting MYmCategories...")
    MYmCategory.resetMapping map { _ =>
      Redirect(routes.SysMarket.showYmCats())
        .flashing("success" -> "Все категории удалены.")
    }
  }

  /** Импорт дерева категорий из io.suggest.ym.cat.YmCategory.CAT_TREE. */
  def importYmCatsSubmit = IsSuperuser.async { implicit request =>
    // TODO WARNING DANGER ACHTUNG Эту функцию надо выпилить после запуска.
    warn("Inserting categories into MYmCategories...")
    MYmCategory.insertYmCats.map { _ =>
      Redirect(routes.SysMarket.showYmCats())
        .flashing("succes" -> "Импорт сделан.")
    }
  }


  // ======================================================================
  // отладка email-сообщений

  /** Отобразить html/txt email-сообщение активации без отправки куда-либо чего-либо. Нужно для отладки. */
  def showShopEmailActMsgHtml(shopId: String, isHtml: Boolean) = IsSuperuser.async { implicit request =>
    for {
      mshop <- getShopById(shopId).map(_.get)
      mmart <- getMartById(mshop.adn.supId.get).map(_.get)
    } yield {
      val eAct = EmailActivation("test@test.com", id = Some("asdQE123_"))
      if (isHtml)
        Ok(lk.mart.shop.emailShopInviteTpl(mmart, mshop, eAct))
      else
        Ok(views.txt.market.lk.mart.shop.emailShopInviteTpl(mmart, mshop, eAct) : String)
    }
  }


  /** Отобразить технический список рекламных карточек узла. */
  def showAdnNodeAds(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    val madsFut = MAd.findForProducer(adnId)
    val adFreqsFut = MAdStat.findAdByActionFreqs(adnId)
    for {
      adFreqs  <- adFreqsFut
      mads     <- madsFut
    } yield {
      Ok(showAdnNodeAdsTpl(mads, request.adnNode, adFreqs))
    }
  }

  /** Отобразить email-уведомление об отключении указанной рекламы. */
  def showShopEmailAdDisableMsg(adId: String, isHtml: Boolean) = IsSuperuser.async { implicit request =>
    MAd.getById(adId) flatMap {
      case Some(mad) =>
        val mmartFut = mad.receivers.headOption match {
          case Some(rcvr) => MAdnNode.getById(rcvr._2.receiverId)
          case None       => MAdnNode.getAll(maxResults = 1).map { _.headOption }
        }
        for {
          mshopOpt <- getShopById(mad.producerId)
          mmartOpt <- mmartFut
        } yield {
          val reason = "Причина отключения ТЕСТ причина отключения 123123 ТЕСТ причина отключения."
          val adOwner = mshopOpt.orElse(mmartOpt).get
          if (isHtml) {
            Ok(views.html.market.lk.shop.ad.emailAdDisabledByMartTpl(mmartOpt.get, adOwner, mad, reason))
          } else {
            Ok(views.txt.market.lk.shop.ad.emailAdDisabledByMartTpl(mmartOpt.get, adOwner, mad, reason): String)
          }
        }

      case None => NotFound("ad not found: " + adId)
    }
  }

  /** Отрендериить тела email-сообщений инвайта передачи прав на ТЦ. */
  def showMartEmailInvite(martId: String, isHtml: Boolean) = IsSuperuser.async { implicit request =>
    getMartById(martId) map {
      case Some(mmart) =>
        val eAct = EmailActivation("asdasd@kde.org", key=martId, id = Some("123123asdasd_-123"))
        val ctx = implicitly[Context]
        if (isHtml)
          Ok(views.html.market.lk.mart.invite.emailMartInviteTpl(mmart, eAct)(ctx))
        else
          Ok(views.txt.market.lk.mart.invite.emailMartInviteTpl(mmart, eAct)(ctx) : String)

      case None => martNotFound(martId)
    }
  }

}

