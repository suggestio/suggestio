package controllers

import com.google.inject.Inject
import io.suggest.util.MacroLogsImpl
import models.usr.EmailActivation
import play.api.db.DB
import play.twirl.api.Html
import util.acl._
import models._
import util.adv.AdvUtil
import util.mail.IMailerWrapper
import views.html.sys1.market._
import views.html.sys1.market.ad._
import views.html.sys1.market.adn._
import play.api.data._
import play.api.libs.concurrent.Execution.Implicits._
import util.SiowebEsUtil.client
import play.api.Play.current
import scala.concurrent.Future
import play.api.mvc.{Result, Call, AnyContent}
import play.api.i18n.MessagesApi
import controllers.sysctl._
import sysctl.SysMarketUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.02.14 17:21
 * Description: Тут управление компаниями, торговыми центрами и магазинами.
 */
class SysMarket @Inject() (
  override val messagesApi: MessagesApi,
  override val mailer: IMailerWrapper
)
  extends SioControllerImpl with MacroLogsImpl with ShopMartCompat with SysNodeInstall with SmSendEmailInvite
{

  import LOGGER._

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


  /** Страница с унифицированным списком узлов рекламной сети в алфавитном порядке с делёжкой по memberType. */
  // TODO stiIdOpt должен быть не id, а конретным экземпляром ShownTypeId.
  def adnNodesList(stiIdOpt: Option[String]) = IsSuperuser.async { implicit request =>
    val companiesFut = MCompany
      .getAll(maxResults = 1000)
      .map { companies =>
        companies.map { c  =>  c.id.get -> c }.toMap
      }
    val adnNodesFut = stiIdOpt match {
      case Some(stiId) =>
        val sargs = new AdnNodesSearchArgs {
          override def shownTypeIds = Seq(stiId)
          override def maxResults = 1000
          override def testNode = None
        }
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
    val companyOptFut = adnNode.getCompany
    for {
      slaves <- slavesFut
      companyOpt <- companyOptFut
    } yield {
      Ok(adnNodeShowTpl(adnNode, slaves, companyOpt))
    }
  }

  /** Безвозвратное удаление узла рекламной сети. */
  def deleteAdnNodeSubmit(adnId: String) = IsSuperuserAdnNodePost(adnId).async { implicit request =>
    import request.adnNode
    adnNode
      .delete
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


  private def maybeSupOpt(supIdOpt: Option[String]): Future[Option[MAdnNode]] = {
    supIdOpt match {
      case Some(supId) => MAdnNodeCache.getById(supId)
      case None => Future successful None
    }
  }
  private def createAdnNodeRender(supOptFut: Future[Option[MAdnNode]], supIdOpt: Option[String], nodeFormM: Form[MAdnNode])
                                 (implicit request: AbstractRequestWithPwOpt[_]): Future[Html] = {
    val companiesFut = MCompany.getAll(maxResults = 100)
    for {
      supOpt    <- supOptFut
      companies <- companiesFut
    } yield {
      createAdnNodeFormTpl(supOpt, nodeFormM, companies)
    }
  }

  /** Страница с формой создания нового узла. */
  def createAdnNode(supIdOpt: Option[String]) = IsSuperuser.async { implicit request =>
    // Генерим stub и втыкаем его в форму, чтобы меньше галочек ставить.
    val dfltFormM = adnNodeFormM.fill(
      MAdnNode(
        adn = AdNetMemberInfo(
          isUser          = false,
          memberType      = AdNetMemberTypes.MART,
          rights          = Set(AdnRights.PRODUCER, AdnRights.RECEIVER),
          shownTypeIdOpt  = Some(AdnShownTypes.MART.name),
          testNode        = false,
          isEnabled       = true,
          sinks           = Set(AdnSinks.SINK_GEO)
        )
      )
    )
    createAdnNodeRender(maybeSupOpt(supIdOpt), supIdOpt, dfltFormM)
      .map { Ok(_) }
  }

  /** Сабмит формы создания нового узла. */
  def createAdnNodeSubmit(supIdOpt: Option[String]) = IsSuperuser.async { implicit request =>
    val supOptFut = maybeSupOpt(supIdOpt)
    adnNodeFormM.bindFromRequest().fold(
      {formWithErrors =>
        val renderFut = createAdnNodeRender(supOptFut, supIdOpt, formWithErrors)
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
  def editAdnNode(adnId: String) = IsSuperuserAdnNodeGet(adnId).async { implicit request =>
    import request.adnNode
    val formFilled = adnNodeFormM.fill(adnNode)
    editAdnNodeBody(adnId, formFilled)
      .map { Ok(_) }
  }

  private def editAdnNodeBody(adnId: String, form: Form[MAdnNode])
                             (implicit request: AbstractRequestForAdnNode[AnyContent]): Future[Html] = {
    MCompany.getAll(maxResults = 1000) map { companies =>
      editAdnNodeFormTpl(request.adnNode, form, companies)
    }
  }

  /** Самбит формы редактирования узла. */
  def editAdnNodeSubmit(adnId: String) = IsSuperuserAdnNodePost(adnId).async { implicit request =>
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


  // Инвайты на управление ТЦ

  /** Рендер страницы с формой инвайта (передачи прав на управление ТЦ). */
  def nodeOwnerInviteForm(adnId: String) = IsSuperuserAdnNodeGet(adnId).async { implicit request =>
    val eActsFut = EmailActivation.findByKey(adnId)
    eActsFut map { eActs =>
      Ok(nodeOwnerInvitesTpl(request.adnNode, nodeOwnerInviteFormM, eActs))
    }
  }

  /** Сабмит формы создания инвайта на управление ТЦ. */
  def nodeOwnerInviteFormSubmit(adnId: String) = IsSuperuserAdnNodePost(adnId).async { implicit request =>
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



  /* Магазины (арендаторы ТЦ). */

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
  def showEmailInviteMsg(adnId: String) = IsSuperuserAdnNode(adnId) { implicit request =>
    import request.adnNode
    val eAct = EmailActivation("test@test.com", id = Some("asdQE123_"))
    Ok(views.html.lk.adn.invite.emailNodeOwnerInviteTpl(adnNode, eAct))
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


  /** Убрать указанную рекламную карточку из выдачи указанного ресивера. */
  def removeAdRcvr(adId: String, rcvrIdOpt: Option[String], r: Option[String]) = IsSuperuser.async { implicit request =>
    val isOkFut = AdvUtil.removeAdRcvr(adId, rcvrIdOpt = rcvrIdOpt)
    lazy val logPrefix = s"removeAdRcvr(ad[$adId]${rcvrIdOpt.fold("")(", rcvr[" + _ + "]")}): "
    val madOptFut = MAd.getById(adId)
    // Радуемся в лог.
    rcvrIdOpt match {
      case Some(rcvrId) => info(logPrefix + "Starting removing for single rcvr...")
      case None         => warn(logPrefix + "Starting removing ALL rcvrs...")
    }
    // Начинаем асинхронно генерить ответ клиенту.
    val rdrToFut: Future[Result] = RdrBackOrFut(r) {
      rcvrIdOpt.fold[Future[Call]] {
        madOptFut map {
          case Some(mad) => routes.SysMarket.showAdnNode(mad.producerId)
          case None => routes.SysMarket.index()
        }
      }
      {rcvrId =>
        val adSearch = new AdSearch {
          override def receiverIds = List(rcvrId)
        }
        val call = routes.SysMarket.showAdnNodeAds(adSearch)
        Future successful call
      }
    }
    // Дождаться завершения всех операций.
    for {
      rdr  <- rdrToFut
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
  def showShopEmailAdDisableMsg(adId: String) = IsSuperuser.async { implicit request =>
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
          Ok(views.html.lk.shop.ad.emailAdDisabledByMartTpl(mmartOpt.get, adOwner, mad, reason))
        }

      case None => NotFound("ad not found: " + adId)
    }
  }

  /** Отрендериить тела email-сообщений инвайта передачи прав на ТЦ. */
  def showNodeOwnerEmailInvite(adnId: String) = IsSuperuserAdnNode(adnId) { implicit request =>
    val eAct = EmailActivation("asdasd@kde.org", key=adnId, id = Some("123123asdasd_-123"))
    val ctx = implicitly[Context]
    Ok(views.html.lk.adn.invite.emailNodeOwnerInviteTpl(request.adnNode, eAct)(ctx))
  }


  /**
   * Выдать sys-страницу относительно указанной карточки.
   * @param adId id рекламной карточки.
   */
  def showAd(adId: String) = IsSuperuserMad(adId).async { implicit request =>
    import request.mad
    val producerOptFut = MAdnNodeCache.getById(mad.producerId)
    for {
      producerOpt <- producerOptFut
    } yield {
      Ok(showAdTpl(mad, producerOpt))
    }
  }


  /** Вывести результат анализа ресиверов рекламной карточки. */
  def analyzeAdRcvrs(adId: String) = IsSuperuserMad(adId).async { implicit request =>
    import request.mad
    val producerOptFut = MAdnNodeCache.getById(mad.producerId)
    val newRcvrsMapFut = producerOptFut flatMap { AdvUtil.calculateReceiversFor(mad, _) }
    // Достаём из кеша узлы.
    val nodesMapFut: Future[Map[String, MAdnNode]] = {
      val adnIds1 = mad.receivers.keySet
      for {
        adns1       <- MAdnNodeCache.multiGet(adnIds1)
        newRcvrsMap <- newRcvrsMapFut
        newAdns     <- MAdnNodeCache.multiGet(newRcvrsMap.keySet -- adnIds1)
      } yield {
        (adns1.iterator ++ newAdns.iterator)
          .map { adnNode => adnNode.id.get -> adnNode }
          .toMap
      }
    }
    for {
      newRcvrsMap <- newRcvrsMapFut
      producerOpt <- producerOptFut
      nodesMap    <- nodesMapFut
    } yield {
      Ok(showAdRcvrsTpl(mad, newRcvrsMap, nodesMap, producerOpt))
    }
  }

  /** Пересчитать и сохранить ресиверы для указанной рекламной карточки. */
  def resetReceivers(adId: String, r: Option[String]) = IsSuperuser.async { implicit request =>
    MAd.getById(adId).flatMap { madOpt =>
      val mad0 = madOpt.get
      AdvUtil.calculateReceiversFor(mad0) flatMap { newRcvrs =>
        MAd.tryUpdate(mad0) { mad =>
          mad.copy(
            receivers = newRcvrs
          )
        } map { _adId =>
          RdrBackOr(r)( routes.SysMarket.showAdnNode(mad0.producerId) )
        }
      }
    }
  }


  /** Очистить полностью таблицу ресиверов. Бывает нужно для временного сокрытия карточки везде.
    * Это действие можно откатить через resetReceivers. */
  def cleanReceivers(adId: String, r: Option[String]) = IsSuperuser.async { implicit request =>
    MAd.getById(adId).flatMap { madOpt =>
      val mad0 = madOpt.get
      MAd.tryUpdate(madOpt.get) { mad =>
        mad.copy(
          receivers = Map.empty
        )
      } map { _adId =>
        RdrBackOr(r) { routes.SysMarket.showAdnNode(mad0.producerId) }
      }
    }
  }

}

