package controllers

import io.suggest.model.geo.CircleGs
import io.suggest.util.MacroLogsImpl
import org.joda.time.DateTime
import play.api.db.DB
import play.twirl.api.HtmlFormat
import util.acl._
import models._
import util.billing.MmpDailyBilling
import views.html.sys1.market._
import play.api.data._, Forms._
import util.FormUtil._
import io.suggest.ym.model.UsernamePw
import play.api.libs.concurrent.Execution.Implicits._
import util.SiowebEsUtil.client
import util.Context
import com.typesafe.plugin.{use, MailerPlugin}
import play.api.Play.{current, configuration}
import scala.concurrent.Future
import io.suggest.ym.model.common.AdnMemberShowLevels.LvlMap_t
import io.suggest.ym.model.common.{NodeConf, AdnMemberShowLevels}
import play.api.mvc.AnyContent
import play.api.i18n.Messages

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.02.14 17:21
 * Description: Тут управление компаниями, торговыми центрами и магазинами.
 */
object SysMarket extends SioController with MacroLogsImpl with ShopMartCompat {

  import LOGGER._

  /** Маппер для метаданных компании. */
  private def companyMetaM = {
    mapping(
      "name" -> companyNameM
    )
    { name => MCompanyMeta.apply(name) }
    { meta => Some(meta.name) }
  }


  /** Маппинг для формы добавления/редактирования компании. */
  def companyFormM = {
    val m = mapping(
      "meta" -> companyMetaM
    )
    { meta => MCompany(meta = meta) }
    { mc   => Some(mc.meta) }
    Form(m)
  }


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
  def companyAddForm(c: Option[MCompany]) = IsSuperuser { implicit request =>
    val form = c.fold(companyFormM) { mc => companyFormM fill mc }
    Ok(company.companyAddFormTpl(form))
  }

  /** Самбит формы добавления новой компании. */
  def companyAddFormSubmit = IsSuperuser.async { implicit request =>
    companyFormM.bindFromRequest.fold(
      {formWithErrors =>
        debug(s"companyAddFormSubmit(): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        NotAcceptable(company.companyAddFormTpl(formWithErrors))
      },
      {mc =>
        mc.save.map { companyId =>
          Redirect(routes.SysMarket.companyShow(companyId))
        }
      }
    )
  }

  /** Отобразить информацию по указанной компании.
    * @param companyId Числовой id компании.
    */
  def companyShow(companyId: String) = IsSuperuserCompany(companyId).async { implicit request =>
    MAdnNode.findByCompanyId(companyId, maxResults = 100) map { adnms =>
      Ok(company.companyShowTpl(request.company, adnms))
    }
  }


  /** Отрендерить страницу с формой редактирования компании. */
  def companyEditForm(companyId: String, r: Option[String]) = IsSuperuserCompany(companyId).apply { implicit request =>
    import request.{company => mc}
    val form = companyFormM fill mc
    Ok(company.companyEditFormTpl(mc, form, r))
  }

  /** Сабмит формы редактирования компании. */
  def companyEditFormSubmit(companyId: String, r: Option[String]) = IsSuperuserCompany(companyId).async { implicit request =>
    import request.{company => mc}
    companyFormM.bindFromRequest.fold(
      {formWithErrors =>
        debug(s"companyEditFormSubmit($companyId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        NotAcceptable(company.companyEditFormTpl(mc, formWithErrors, r))
      },
      {mc2 =>
        // Собираем новый инстанс компании.
        val mc3 = updateCompany(mc, mc2)
        mc3.save map { _companyId =>
          RdrBackOr(r) { routes.SysMarket.companyShow(_companyId) }
            .flashing("success" -> "Изменения сохранены.")
        }
      }
    )
  }

  /** Функция для обновления компании отмаппленными данными companyFormM.
    * @param mc Текущий (исходный) инстанс компании.
    * @param mc2 Результат маппинга companyFormM.
    * @return Новый инстанс, содержащий в себе былые данные, местами перезаписанные новыми.
    */
  def updateCompany(mc: MCompany, mc2: MCompany): MCompany = {
    mc.copy(
      meta = mc.meta.copy(
        name = mc2.meta.name
      )
    )
  }

  /** Админ приказал удалить указанную компанию. */
  def companyDeleteSubmit(companyId: String) = IsSuperuserCompany(companyId).async { implicit request =>
    request.company
      .delete
      .flatMap { isDeleted =>
        request.company.eraseResources
          .map { _ => isDeleted }
      }
      .filter(identity)
      .map { _ =>
        Redirect(routes.SysMarket.companiesList())
          .flashing("success" -> "Компания удалёна.")
      }
      .recover {
        case nse: NoSuchElementException =>
          warn(s"deleteAdnNodeSubmit($companyId): Node not found. Anyway, resources re-erased.")
          IsSuperuserCompany.companyNotFound(companyId)
      }
  }


  /* Унифицированные узлы ADN */
  import views.html.sys1.market.adn._

  /** Страница с унифицированным списком узлов рекламной сети в алфавитном порядке с делёжкой по memberType.  */
  def adnNodesList(stiIdOpt: Option[String]) = IsSuperuser.async { implicit request =>
    val companiesFut = MCompany
      .getAll(maxResults = 1000)
      .map { companies =>
        companies.map { c  =>  c.id.get -> c }.toMap
      }
    val adnNodesFut = stiIdOpt match {
      case Some(stiId) =>
        val sargs = MAdnNodeSearch(
          shownTypes = Seq(AdnShownTypes.withName(stiId)),
          maxResults = 1000,
          testNode = None
        )
        MAdnNode.dynSearch(sargs)
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
    val slavesFut = MAdnNode.findBySupId(adnId, maxResults = 100)
    val companyOptFut = MCompany.getById(adnNode.companyId)
    for {
      slaves <- slavesFut
      companyOpt <- companyOptFut
    } yield {
      Ok(adnNodeShowTpl(adnNode, slaves, companyOpt))
    }
  }

  /** Безвозвратное удаление узла рекламной сети. */
  def deleteAdnNodeSubmit(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    import request.adnNode
    adnNode
      .delete
      .flatMap { isDeleted =>
        adnNode.eraseResources
          .map { _ => isDeleted }
      }
      .filter(identity)
      .map { _ =>
        Redirect(routes.SysMarket.adnNodesList())
          .flashing("success" -> "Узел ADN удалён.")
      }
      .recover {
        case nse: NoSuchElementException =>
          warn(s"deleteAdnNodeSubmit($adnId): Node not found. Anyway, resources re-erased.")
          IsSuperuserAdnNode.nodeNotFound(adnId)
      }
  }


  /** Форма для маппинг метаданных произвольного узла ADN. */
  private def adnNodeMetaM = mapping(
    "name"      -> nameM,
    "descr"     -> publishedTextOptM,
    "town"      -> townOptM,
    "address"   -> addressOptM,
    "phone"     -> phoneOptM,
    "floor"     -> floorOptM,
    "section"   -> sectionOptM,
    "siteUrl"   -> urlStrOptM,
    "color"     -> colorOptM,
    "location"  -> latLng2geopointOptM
  )
  {(name, descr, town, address, phone, floor, section, siteUrl, color, locationOpt) =>
    AdnMMetadata(
      name    = name,
      description = descr,
      town    = town,
      address = address,
      phone   = phone,
      floor   = floor,
      section = section,
      siteUrl = siteUrl,
      color   = color,
      location = locationOpt
    )
  }
  {meta =>
    import meta._
    Some((name, description, town, address, phone, floor, section, siteUrl, color, location))
  }

  private def adnRightsM: Mapping[Set[AdnRight]] = {
    import AdnRights._
    mapping(
      PRODUCER.longName -> boolean,
      RECEIVER.longName -> boolean,
      SUPERVISOR.longName -> boolean
    )
    {(isProd, isRcvr, isSup) =>
      var acc: List[AdnRight] = Nil
      if (isProd) acc ::= PRODUCER
      if (isRcvr) acc ::= RECEIVER
      if (isSup)  acc ::= SUPERVISOR
      acc.toSet
    }
    {rights =>
      val isProd = rights contains PRODUCER
      val isRcvr = rights contains RECEIVER
      val isSup  = rights contains SUPERVISOR
      Some((isProd, isRcvr, isSup))
    }
  }

  private def slsStrM: Mapping[LvlMap_t] = {
    text(maxLength = 256)
      .transform[LvlMap_t](
        {raw =>
          raw.split("\\s*,\\s*")
            .toSeq
            .map { one =>
              val Array(slRaw, slMaxRaw) = one.split("\\s*=\\s*")
              val sl: AdShowLevel = AdShowLevels.withName(slRaw)
              sl -> slMaxRaw.toInt
            }
            .filter(_._2 > 0)
            .toMap
        },
        {sls =>
          val raws = sls.map {
            case (sl, slMax)  =>  s"${sl.name} = $slMax"
          }
          raws.mkString(", ")
        }
      )
  }

  private def adnSlInfoM: Mapping[AdnMemberShowLevels] = {
    val slsStrOptM: Mapping[LvlMap_t] = default(slsStrM, Map.empty)
    mapping(
      "in"  -> slsStrOptM,
      "out" -> slsStrOptM
    )
    { AdnMemberShowLevels.apply }
    { AdnMemberShowLevels.unapply }
  }

  /** Доступная узлу пути рекламной выдачи. */
  private def adnSinksM: Mapping[Set[AdnSink]] = {
    mapping(
      AdnSinks.SINK_WIFI.longName -> boolean,
      AdnSinks.SINK_GEO.longName  -> boolean
    )
    {(isWifi, isGeo) =>
      var acc = List.empty[AdnSink]
      if (isWifi)
        acc ::= AdnSinks.SINK_WIFI
      if (isGeo)
        acc ::= AdnSinks.SINK_GEO
      acc.toSet
    }
    {sinks =>
      val isWifi = sinks contains AdnSinks.SINK_WIFI
      val isGeo  = sinks contains AdnSinks.SINK_GEO
      Some((isWifi, isGeo))
    }
  }

  /** Маппинг для adn-полей формы adn-узла. */
  private def adnMemberM: Mapping[AdNetMemberInfo] = mapping(
    "memberType"    -> adnMemberTypeM,
    "isEnabled"     -> boolean,
    "shownTypeIdOpt" -> adnShownTypeIdOptM,
    "rights"        -> adnRightsM,
    "sls"           -> adnSlInfoM,
    "supId"         -> optional(esIdM),
    "advDelegate"   -> optional(esIdM),
    "testNode"      -> boolean,
    "sink"          -> adnSinksM
  )
  {(mt, isEnabled, shownTypeIdOpt, rights, sls, supId, advDgOpt, isTestNode, sinks) =>
    mt.getAdnInfoDflt.copy(
      isEnabled = isEnabled,
      rights    = rights,
      shownTypeIdOpt = shownTypeIdOpt,
      showLevelsInfo = sls,
      supId     = supId,
      advDelegate = advDgOpt,
      testNode  = isTestNode,
      sinks     = sinks
    )
  }
  {anmi =>
    import anmi._
    Some((memberType, isEnabled, Some(shownTypeId), rights, showLevelsInfo, supId, advDelegate, testNode, sinks))
  }


  /** Маппинг для конфига ноды. */
  private def nodeConfM: Mapping[NodeConf] = {
    val intSetM = text(maxLength = 1024)
      .transform [Set[Int]] (
        {raw =>
          raw.trim
            .split("\\s*,\\s*")
            .foldLeft [List[Int]] (Nil) { (acc, vRaw) =>
              val vOpt: Option[Int] = try {
                val blockId = vRaw.toInt
                BlocksConf.apply(blockId)   // Проверяем, есть ли блок с указанным id.
                Some(blockId)
              } catch {
                case ex: NumberFormatException =>
                  warn("Cannot parse block id as integer: " + vRaw)
                  None
                case ex: NoSuchElementException =>
                  warn("Block id not found: " + vRaw)
                  None
              }
              vOpt match {
                case Some(blockId) => blockId :: acc
                case None => acc
              }
            }
            .toSet
        },
        { _.mkString(", ") }
      )
    mapping(
      "withBlocks" -> default [Set[Int]] (intSetM, Set.empty)
    )
    { NodeConf.apply }
    { NodeConf.unapply }
  }

  /** Маппер для поля, содержащего список id юзеров. */
  private def personIdsM: Mapping[Set[String]] = {
    text(maxLength = 1024)
      .transform[Set[String]](
        {s => s.trim.split("\\s*[,;]\\s*").filter(!_.isEmpty).toSet },
        { _.mkString(", ") }
      )
  }

  private def adnKM  = "adn" -> adnMemberM
  private def metaKM = "meta" -> adnNodeMetaM
  private def confKM = "conf" -> nodeConfM
  private def personIdsKM = "personIds" -> personIdsM

  /** Генератор маппингов для формы добавления/редактирования рекламного узла. */
  def getAdnNodeFormM(companyM: Mapping[String]): Form[MAdnNode] = {
    Form(mapping(
      "companyId" -> companyM, adnKM, metaKM, confKM, personIdsKM
    )
    {(companyId, anmi, meta, conf, personIds) =>
      MAdnNode(
        meta = meta,
        companyId = companyId,
        adn = anmi,
        conf = conf,
        personIds = personIds
      )
    }
    {adnNode =>
      import adnNode._
      Some((companyId, adn, meta, conf, personIds))
    })
  }
  private def adnNodeFormM = getAdnNodeFormM(esIdM)

  private def maybeSupOpt(supIdOpt: Option[String]): Future[Option[MAdnNode]] = {
    supIdOpt match {
      case Some(supId) => MAdnNodeCache.getById(supId)
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
      {adnNodeRaw =>
        // Запиливаем sup id в будущий node.
        val adnNode = adnNodeRaw.copy(
          adn = adnNodeRaw.adn.copy(
            supId = supIdOpt
          )
        )
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
              .flashing("success" -> s"Создан узел сети: $adnId")
          }
        }
      }
    )
  }


  /** Страница с формой редактирования узла ADN. */
  def editAdnNode(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    import request.adnNode
    val formFilled = adnNodeFormM.fill(adnNode)
    editAdnNodeBody(adnId, formFilled)
      .map { Ok(_) }
  }

  private def editAdnNodeBody(adnId: String, form: Form[MAdnNode])
                             (implicit request: AbstractRequestForAdnNode[AnyContent]): Future[HtmlFormat.Appendable] = {
    MCompany.getAll(maxResults = 1000) map { companies =>
      editAdnNodeFormTpl(request.adnNode, form, companies)
    }
  }

  /** Самбит формы редактирования узла. */
  def editAdnNodeSubmit(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    import request.adnNode
    val formBinded = adnNodeFormM.bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        debug(s"editAdnNodeSubmit($adnId): Failed to bind form: ${formatFormErrors(formWithErrors)}")
        editAdnNodeBody(adnId, formWithErrors)
          .map(NotAcceptable(_))
      },
      {adnNode2 =>
        val supExistsFut: Future[Boolean] = adnNode2.adn.supId.fold
          { Future successful true }
          { supId => MAdnNodeCache.getById(supId).map(_.isDefined) }
        supExistsFut flatMap {
          case true =>
            MAdnNode.tryUpdate(adnNode) { updateAdnNode(_, adnNode2) }
              .map { _ =>
                Redirect(routes.SysMarket.showAdnNode(adnId))
                  .flashing("success" -> "Изменения сохранены")
              }

          case false =>
            val formWithErrors = formBinded.withError("adn.supId", "error.invalid")
            editAdnNodeBody(adnId, formWithErrors)
              .map(NotAcceptable(_))
        }
      }
    )
  }

  /** Накатить отмаппленные изменения на существующий интанс узла, породив новый интанс.*/
  def updateAdnNode(adnNode: MAdnNode, adnNode2: MAdnNode): MAdnNode = {
    adnNode.copy(
      companyId = adnNode2.companyId,
      personIds = adnNode2.personIds,
      meta = adnNode.meta.copy(
        name    = adnNode2.meta.name,
        description = adnNode2.meta.description,
        town    = adnNode2.meta.town,
        address = adnNode2.meta.address,
        phone   = adnNode2.meta.phone,
        floor   = adnNode2.meta.floor,
        section = adnNode2.meta.section,
        siteUrl = adnNode2.meta.siteUrl,
        color   = adnNode2.meta.color,
        location = adnNode2.meta.location
      ),
      adn = adnNode.adn.copy(
        memberType  = adnNode2.adn.memberType,
        rights      = adnNode2.adn.rights,
        shownTypeIdOpt = adnNode2.adn.shownTypeIdOpt,
        isEnabled   = adnNode2.adn.isEnabled,
        showLevelsInfo = adnNode2.adn.showLevelsInfo,
        supId       = adnNode2.adn.supId,
        advDelegate = adnNode2.adn.advDelegate,
        testNode    = adnNode2.adn.testNode,
        sinks       = adnNode2.adn.sinks
      ),
      conf = adnNode.conf.copy(
        withBlocks = adnNode2.conf.withBlocks
      )
    )
  }

  /* Торговые центры и площади. */

  private def martNotFound(martId: String) = NotFound("Mart not found: " + martId)


  // Инвайты на управление ТЦ

  private def nodeOwnerInviteFormM = Form(
    "email" -> email
  )

  /** Рендер страницы с формой инвайта (передачи прав на управление ТЦ). */
  def nodeOwnerInviteForm(adnId: String) = IsSuperuser.async { implicit request =>
    val eActsFut = EmailActivation.findByKey(adnId)
    MAdnNodeCache.getById(adnId) flatMap {
      case Some(adnNode) =>
        eActsFut map { eActs =>
          Ok(nodeOwnerInvitesTpl(adnNode, nodeOwnerInviteFormM, eActs))
        }
      case None => martNotFound(adnId)
    }
  }

  /** Сабмит формы создания инвайта на управление ТЦ. */
  def nodeOwnerInviteFormSubmit(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    import request.adnNode
    nodeOwnerInviteFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"martInviteFormSubmit($adnId): Failed to bind form: ${formWithErrors.errors}")
        EmailActivation.findByKey(adnId) map { eActs =>
          NotAcceptable(nodeOwnerInvitesTpl(adnNode, formWithErrors, eActs))
        }
      },
      {email1 =>
        val eAct = EmailActivation(email=email1, key = adnId)
        eAct.save.map { eActId =>
          val eact2 = eAct.copy(
            id = Some(eActId)
          )
          sendEmailInvite(eact2, adnNode)
          // Письмо отправлено, вернуть админа назад в магазин
          Redirect(routes.SysMarket.showAdnNode(adnId))
            .flashing("success" -> ("Письмо с приглашением отправлено на " + email1))
        }
      }
    )
  }


  /** Выслать письмо активации. */
  def sendEmailInvite(ea: EmailActivation, adnNode: MAdnNode)(implicit request: AbstractRequestWithPwOpt[AnyContent]) {
    // Собираем и отправляем письмо адресату
    val mail = use[MailerPlugin].email
    val ctx = implicitly[Context]   // нано-оптимизация: один контекст для обоих шаблонов.
    mail.setSubject("Suggest.io | " + Messages("Your")(ctx.lang) + " " + Messages("amt.of.type." + adnNode.adn.shownTypeId)(ctx.lang))
    mail.setFrom("no-reply@suggest.io")
    mail.setRecipient(ea.email)
    mail.send(
      bodyText = views.txt.market.lk.adn.invite.emailNodeOwnerInviteTpl(adnNode, ea)(ctx),
      bodyHtml = views.html.market.lk.adn.invite.emailNodeOwnerInviteTpl(adnNode, ea)(ctx)
    )
  }


  /* Магазины (арендаторы ТЦ). */

  /** Рендер ошибки, если магазин не найден в базе. */
  private def shopNotFound(shopId: String) = NotFound("Shop not found: " + shopId)


  /* Ссылки на прайс-листы магазинов, а именно их изменение. */

  /** Маппинг для формы добавления/редактирования ссылок на прайс-листы. */
  private def splFormM = Form(mapping(
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
  def showEmailInviteMsg(adnId: String, isHtml: Boolean) = IsSuperuserAdnNode(adnId) { implicit request =>
    import request.adnNode
    val eAct = EmailActivation("test@test.com", id = Some("asdQE123_"))
    if (isHtml)
      Ok(views.html.market.lk.adn.invite.emailNodeOwnerInviteTpl(adnNode, eAct))
    else
      Ok(views.txt.market.lk.adn.invite.emailNodeOwnerInviteTpl(adnNode, eAct) : String)
  }


  /** Отобразить технический список рекламных карточек узла. */
  def showAdnNodeAds(a: AdSearch) = IsSuperuser.async { implicit request =>
    // Ищем все рекламные карточки, подходящие под запрос.
    // TODO Нужна устойчивая сортировка.
    val madsFut = MAd.dynSearch(a)
    // Узнаём текущий узел на основе запроса. TODO Кривовато это как-то, может стоит через аргумент передавать?
    val adnNodeIdOpt = a.producerIds.headOption orElse a.receiverIds.headOption
    val adFreqsFut = adnNodeIdOpt
      .fold [Future[MAdStat.AdFreqs_t]] (Future successful Map.empty) { MAdStat.findAdByActionFreqs }
    val adnNodeOptFut: Future[Option[MAdnNode]] = {
      adnNodeIdOpt.fold (Future successful Option.empty[MAdnNode]) { MAdnNodeCache.getById }
    }
    // Собираем карту размещений рекламных карточек.
    val ad2advMapFut = madsFut map { mads =>
      lazy val adIds = mads.flatMap(_.id)
      val advs: Seq[MAdvI] = if (a.receiverIds.nonEmpty) {
        // Ищем все размещения имеющихся карточек у запрошенных ресиверов.
        DB.withConnection { implicit c =>
          MAdvOk.findByAdIdsAndRcvrs(adIds, rcvrIds = a.receiverIds) ++
            MAdvReq.findByAdIdsAndRcvrs(adIds, rcvrIds = a.receiverIds)
        }
      } else if (a.producerIds.nonEmpty) {
        // Ищем размещения карточек для продьюсера.
        DB.withConnection { implicit c =>
          MAdvOk.findByAdIdsAndProducersOnline(adIds, prodIds = a.producerIds, isOnline = true) ++
            MAdvReq.findByAdIdsAndProducers(adIds, prodIds = a.producerIds)
        }
      } else {
        Nil
      }
      advs.groupBy(_.adId)
    }
    // Собираем ресиверов рекламных карточек.
    val rcvrsFut: Future[Map[String, Seq[MAdnNode]]] = if (a.receiverIds.nonEmpty) {
      // Используем только переданные ресиверы.
      Future
        .traverse(a.receiverIds) { MAdnNodeCache.getById }
        .flatMap { rcvrOpts =>
          val rcvrs = rcvrOpts.flatten
          madsFut map { mads =>
            mads.flatMap(_.id)
              .map { adId => adId -> rcvrs }
              .toMap
          }
        }
    } else {
      // Собираем всех ресиверов со всех рекламных карточек. Делаем это через биллинг, т.к. в mad только текущие ресиверы.
      ad2advMapFut.flatMap { ad2advsMap =>
        val allRcvrIds = ad2advsMap.foldLeft(List.empty[String]) {
          case (acc0, (_, advs)) =>
            advs.foldLeft(acc0) {
              (acc1, adv) => adv.rcvrAdnId :: acc1
            }
        }
        MAdnNodeCache.multiGet(allRcvrIds.toSet) map { allRcvrs =>
          // Список ресиверов конвертим в карту ресиверов.
          val rcvrsMap = allRcvrs.map { rcvr => rcvr.id.get -> rcvr }.toMap
          // Заменяем в исходной карте ad2advs списки adv на списки ресиверов.
          ad2advsMap.mapValues { advs =>
            advs.flatMap {
              adv  =>  rcvrsMap get adv.rcvrAdnId
            }
          }
        }
      }
    }
    // Планируем рендер страницы-результата, когда все данные будут собраны.
    for {
      adFreqs       <- adFreqsFut
      mads          <- madsFut
      adnNodeOpt    <- adnNodeOptFut
      rcvrs         <- rcvrsFut
      ad2advMap     <- ad2advMapFut
    } yield {
      Ok(showAdnNodeAdsTpl(mads, adnNodeOpt, adFreqs, rcvrs, a, ad2advMap))
    }
  }


  /** Причина hard-отказа в размещении со стороны suggest.io, а не узла.
    * Потом надо это заменить на нечто иное: чтобы суперюзер s.io вводил причину. */
  val SIOM_REFUSE_REASON = configuration.getString("sys.m.ad.hard.refuse.reason") getOrElse "Refused by suggest.io."

  /** Убрать указанную рекламную карточку из выдачи указанного ресивера. */
  def removeAdRcvr(adId: String, rcvrId: String, r: Option[String]) = IsSuperuser.async { implicit request =>
    lazy val logPrefix = s"removeAdRcvr(ad[$adId], rcvr[$rcvrId]): "
    // Надо убрать указанного ресиверов из списка ресиверов
    val isOkFut = MAd.getById(adId) flatMap {
      case Some(mad) =>
        mad.copy(
          receivers = mad.receivers.filterKeys(_ != rcvrId)
        ) .save
          .map { _ => true }
      case None =>
        warn(logPrefix + "MAd not found: " + adId)
        Future successful false
    }
    // Надо убрать карточку из текущего размещения на узле, если есть: из advOk и из advReq.
    DB.withTransaction { implicit c =>
      // Резать как online, так и в очереди на публикацию.
      MAdvOk.findNotExpiredByAdIdAndRcvr(adId, rcvrId = rcvrId, policy = SelectPolicies.UPDATE)
        .foreach { advOk =>
          trace(s"${logPrefix}offlining advOk[${advOk.id.get}]...")
          advOk.copy(dateEnd = DateTime.now, isOnline = false).saveUpdate
        }
      // Запросы размещения переколбашивать в refused с возвратом бабла.
      MAdvReq.findByAdIdAndRcvr(adId, rcvrId = rcvrId, policy = SelectPolicies.UPDATE)
        .foreach { madvReq =>
          trace(s"${logPrefix}refusing advReq[${madvReq.id.get}]...")
          // TODO Нужно как-то управлять причиной выпиливания. Этот action работает через POST, поэтому можно замутить форму какую-то.
          MmpDailyBilling.refuseAdvReq(madvReq, SIOM_REFUSE_REASON)
        }
    }
    // Дождаться завершения остальных операций.
    for {
      rdr  <- RdrBackOr(r) { routes.SysMarket.showAdnNodeAds(AdSearch(receiverIds = List(rcvrId))) }
      isOk <- isOkFut
    } yield {
      // Вернуть редирект с результатом работы.
      val flasher = if (isOk) {
        "success" -> "Карточка убрана из выдачи."
      } else {
        "error"   -> "Карточка не найдена."
      }
      rdr.flashing(flasher)
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
  def showNodeOwnerEmailInvite(adnId: String, isHtml: Boolean) = IsSuperuser.async { implicit request =>
    MAdnNodeCache.getById(adnId) map {
      case Some(adnNode) =>
        val eAct = EmailActivation("asdasd@kde.org", key=adnId, id = Some("123123asdasd_-123"))
        val ctx = implicitly[Context]
        if (isHtml)
          Ok(views.html.market.lk.adn.invite.emailNodeOwnerInviteTpl(adnNode, eAct)(ctx))
        else
          Ok(views.txt.market.lk.adn.invite.emailNodeOwnerInviteTpl(adnNode, eAct)(ctx) : String)

      case None => martNotFound(adnId)
    }
  }

}

