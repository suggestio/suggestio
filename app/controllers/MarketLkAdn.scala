package controllers

import util.billing.Billing
import util.qsb.AdSearch
import util.PlayMacroLogsImpl
import util.acl._
import models._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import scala.concurrent.Future
import io.suggest.ym.model.common.EMAdnMMetadataStatic.META_FLOOR_ESFN
import io.suggest.model.EsModel
import views.html.market.lk.adn._, _node._
import io.suggest.ym.model.MAdnNode
import play.api.data.Form
import play.api.data.Forms._
import util.FormUtil._
import play.api.libs.json._
import io.suggest.ym.model.common.AdShowLevels
import play.api.db.DB
import play.api.mvc.{AnyContent, Result}
import play.api.mvc.Security.username

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 11:18
 * Description: Унифицированные части личного кабинета.
 */
object MarketLkAdn extends SioController with PlayMacroLogsImpl with BruteForceProtect {

  import LOGGER._

  /**
   * Отрендерить страницу ЛК какого-то узла рекламной сети. Экшен различает свои и чужие узлы.
   * @param adnId id узла.
   * @param newAdIdOpt Костыль: если была добавлена рекламная карточка, то она должна отобразится сразу,
   *                   независимо от refresh в индексе. Тут её id.
   * @param povAdnIdOpt С точки зрения какого узла идёт просмотр указанного узла.
   *                    Выверенное значение это аргумента можно получить из request.povAdnNodeOpt.
   */
  def showAdnNode(adnId: String, newAdIdOpt: Option[String], povAdnIdOpt: Option[String]) = {
    AdnNodeAccess(adnId, povAdnIdOpt).async { implicit request =>
      import request.{adnNode, isMyNode}
      // Супервайзинг узла приводит к рендеру ещё одного виджета
      val slavesFut: Future[Seq[MAdnNode]] = if(isMyNode && request.adnNode.adn.isSupervisor) {
        MAdnNode.findBySupId(adnId)
      } else {
        Future successful Nil
      }
      // Нужно ли отображать кнопку назад? Да, если у юзера есть ещё узлы.
      val showBackBtnFut = if (isMyNode) {
        // TODO Надо бы кеширование тут.
        MAdnNode.countByPersonId(request.pwOpt.get.personId)
          .map { _ >= 2L }
      } else {
        Future successful true
      }
      // Для узла нужно отобразить его рекламу.
      val madsFut: Future[Seq[MAd]] = if (isMyNode) {
        // Это свой узел. Нужно в реалтайме найти рекламные карточки и проверить newAdIdOpt.
        val prodAdsFut = MAd.findForProducerRt(adnId)
        // Бывает, что добавлена новая карточка (но индекс ещё не сделал refresh). Нужно её найти и отобразить:
        val extAdOptFut = newAdIdOpt match {
          case Some(newAdId) =>
            MAd.getById(newAdId)
              .map { _.filter { mad =>
                mad.producerId == adnId  ||  mad.receivers.valuesIterator.exists(_.receiverId == adnId)
              } }
          // Нет id new-карточки -- нет и самой карточки.
          case _ => Future successful None
        }
        for {
          prodAds  <- prodAdsFut
          extAdOpt <- extAdOptFut
        } yield {
          // Если есть карточка в extAdOpt, то надо добавить её в начало списка, который отсортирован по дате создания.
          if (extAdOpt.isDefined  &&  prodAds.headOption.flatMap(_.id) != extAdOpt.flatMap(_.id)) {
            extAdOpt.get :: prodAds
          } else {
            prodAds
          }
        }
      } else {
        // Это чужой узел. Нужно отобразить только рекламу, отправленную на размещение pov-узлу.
        request.povAdnNodeOpt match {
          // Есть pov-узел, и юзер является админом оного. Нужно поискать рекламу, созданную на adnId и размещенную на pov-узле.
          case Some(povAdnNode) =>
            // Вычислить взаимоотношения между двумя узлами через список ad_id
            val adIds = DB.withConnection { implicit c =>
              MAdv.findActualAdIdsBetweenNodes(MAdvModes.busyModes, adnId, rcvrId = povAdnNode.id.get)
            }
            MAd.multiGet(adIds)

          // pov-узел напрочь отсутствует. Нечего отображать.
          case None =>
            debug(s"showAdnNode($adnId, pov=$povAdnIdOpt): pov node is empty, no rcvr, no ads.")
            Future successful Nil
        }
      }
      // Нужно узнать, есть ли рекламодатели у указанного узла.
      val (advReqsCount, advertisersFut): (Long, Future[Seq[MAdnNode]]) = if(isMyNode && adnNode.adn.isReceiver) {
        val syncResult = DB.withConnection { implicit c =>
          val okAdnIds = MAdvOk.findAllProducersForRcvr(adnId)
          val reqAdnIds = MAdvReq.findAllProducersForRcvr(adnId)
          val reqsCount = MAdvReq.countForRcvr(adnId)
          (okAdnIds, reqAdnIds, reqsCount)
        }
        val (okAdnIds, reqAdnIds, reqsCount) = syncResult
        val advAdnIds = (okAdnIds ++ reqAdnIds).distinct
        (reqsCount, MAdnNode.multiGet(advAdnIds)) // TODO Opt надо задействовать кеш в multiget'e.
      } else {
        (0L, Future.successful(Nil))
      }
      val ad2advMap = {
        val myAdnId = request.myNodeId
        val busyAdvs = DB.withConnection { implicit c =>
          val busyAdsOk = MAdvOk.findNotExpiredRelatedTo(myAdnId)
          val busyAdsReq = MAdvReq.findNotExpiredRelatedTo(myAdnId)
          Seq(busyAdsOk, busyAdsReq)
        }
        advs2adIdMap(busyAdvs : _*)
      }
      // Надо ли отображать кнопку "управление" под карточками? Да, если есть баланс и контракт.
      val canAdv: Boolean = isMyNode && adnNode.adn.isProducer && {
        DB.withConnection { implicit c =>
          MBillContract.hasActiveForNode(adnId)  &&  MBillBalance.hasForNode(adnId)
        }
      }
      // Дождаться всех фьючерсов и отрендерить результат.
      for {
        mads        <- madsFut
        slaves      <- slavesFut
        advertisers <- advertisersFut
        showBackBtn <- showBackBtnFut
      } yield {
        Ok(adnNodeShowTpl(
          node          = adnNode,
          mads          = mads,
          slaves        = slaves,
          isMyNode      = isMyNode,
          advertisers   = advertisers,
          povAdnIdOpt   = request.povAdnNodeOpt.flatMap(_.id),
          advReqsCount  = advReqsCount,
          canAdv        = canAdv,
          ad2advMap     = ad2advMap,
          showBackBtn   = showBackBtn
        ))
      }
    }
  }


  private def advs2adIdMap(advss: Seq[MAdvI] *): Map[String, MAdvI] = {
    advss.foldLeft [List[(String, MAdvI)]] (Nil) { (acc1, advs) =>
      advs.foldLeft(acc1) { (acc2, adv) =>
        adv.adId -> adv  ::  acc2
      }
    }.toMap
  }

  
  /**
   * Рендер страницы со списком подчинённых узлов.
   * @param adnId id ТЦ
   * @param sortByRaw Сортировка магазинов по указанному полю. Если не задано, то порядок не определён.
   * @param isReversed Если true, то будет сортировка в обратном порядке. Иначе в прямом.
   */
  def showSlaves(adnId: String, sortByRaw: Option[String], isReversed: Boolean) = IsAdnNodeAdmin(adnId).async { implicit request =>
    val sortBy = sortByRaw.flatMap(NodesSort.handleShopsSortBy)
    MAdnNode.findBySupId(adnId, sortBy, isReversed) map { slaves =>
      Ok(slaveNodesTpl(request.adnNode, slaves))
    }
  }
  
    /** Поисковая форма. Сейчас в шаблонах она не используется, только в контроллере. */
  val searchFormM = Form(
    "q" -> nonEmptyText(maxLength = 64)
  )

  /**
   * Поиск по под-узлам указанного супервизора.
   * @param adnId id ТЦ.
   * @return 200 Отрендеренный список узлов для отображения поверх существующей страницы.
   *         406 С сообщением об ошибке.
   */
  def searchSlaves(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    searchFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"searchSlaves($adnId): Failed to bind search form: ${formatFormErrors(formWithErrors)}")
        NotAcceptable("Bad search request")
      },
      {q =>
        MAdnNode.searchAll(q, supId = Some(adnId)) map { slaves =>
          Ok(_slaveNodesListTpl(slaves))
        }
      }
    )
  }


  // Допустимые значения сортировки при выдаче магазинов.
  object NodesSort extends Enumeration {
    val SORT_BY_A_Z   = Value("a-z")
    val SORT_BY_CAT   = Value("cat")
    val SORT_BY_FLOOR = Value("floor")

    def handleShopsSortBy(sortRaw: String): Option[String] = {
      if (SORT_BY_A_Z.toString equalsIgnoreCase sortRaw) {
        Some(EsModel.NAME_ESFN)
      } else if (SORT_BY_CAT.toString equalsIgnoreCase sortRaw) {
        debug(s"handleShopsSortBy($sortRaw): Not yet implemented.")
        None
      } else if (SORT_BY_FLOOR.toString equalsIgnoreCase sortRaw) {
        Some(META_FLOOR_ESFN)
      } else {
        None
      }
    }
  }



  /** Маппинг формы включения/выключения магазина. */
  private val nodeOnOffFormM = Form(tuple(
    "isEnabled" -> boolean,
    "reason"    -> optional(hideEntityReasonM)
  ))


  /**
   * Рендер блока с формой отключения подчинённого узла.
   * @param adnId id отключаемого узла.
   * @return 200 с формой указания причины отключения узла.
   *         404 если узел не найден.
   */
  def nodeOnOffForm(adnId: String) = CanSuperviseNode(adnId).apply { implicit request =>
    import request.slaveNode
    val formBinded = nodeOnOffFormM.fill((false, slaveNode.adn.disableReason))
    Ok(_nodeOnOffFormTpl(slaveNode, formBinded))
  }

  /**
   * Супервизор подсети включает/выключает состояние узла.
   * @param shopId id узла.
   * @return 200 Ok если всё ок.
   */
  def nodeOnOffSubmit(shopId: String) = CanSuperviseNode(shopId).async { implicit request =>
    nodeOnOffFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"nodeOnOffSubmit($shopId): Bind form failed: ${formatFormErrors(formWithErrors)}")
        NotAcceptable("Bad request body.")
      },
      {case (isEnabled, reason) =>
        request.slaveNode.setIsEnabled(isEnabled, reason) map { _ =>
          val reply = JsObject(Seq(
            "isEnabled" -> JsBoolean(isEnabled),
            "shopId" -> JsString(shopId)
          ))
          Ok(reply)
        }
      }
    )
  }
  
  
  
  /** Форма, которая используется при обработке сабмита о переключении доступности магазину функции отображения рекламы
    * на верхнем уровне ТЦ. */
  private val nodeTopLevelFormM = Form(
    "isEnabled" -> boolean
  )

  /** Владелец ТЦ дергает за переключатель доступности top-level выдачи для магазина. */
  def setSlaveTopLevelAvailable(adnId: String) = CanSuperviseNode(adnId).async { implicit request =>
    nodeTopLevelFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"shopSetTopLevel($adnId): Form bind failed: ${formatFormErrors(formWithErrors)}")
        NotAcceptable("Cannot parse req body.")
      },
      {isTopEnabled =>
        import request.slaveNode
        if (isTopEnabled)
          slaveNode.adn.showLevelsInfo.out += AdShowLevels.LVL_START_PAGE -> 1
        else
          slaveNode.adn.showLevelsInfo.out -= AdShowLevels.LVL_START_PAGE
        slaveNode.save map { _ =>
          Ok("updated ok")
        }
      }
    )
  }



  /**
   * Отобразить страницу по подчинённому узлу.
   * @param adnId id под-узла.
   */
  def showSlave(adnId: String) = CanViewSlave(adnId).async { implicit request =>
    import request.{slaveNode, supNode}
    val req = AdSearch(
      receiverIds = List(request.supNode.id.get),
      producerIds = List(adnId)
    )
    MAd.searchAdsRt(req) map { mads =>
      Ok(showSlaveNodeTpl(msup = supNode, mslave = slaveNode, mads))
    }
  }



  // Обработка инвайтов на управление ТЦ.
  private val nodeOwnerInviteAcceptM = Form(
    "password" -> optional(passwordWithConfirmM)
  )

  /** Рендер страницы с формой подтверждения инвайта на управление ТЦ. */
  def nodeOwnerInviteAcceptForm(martId: String, eActId: String) = nodeOwnerInviteAcceptCommon(martId, eActId) { (eAct, mmart) => implicit request =>
    Ok(invite.inviteAcceptFormTpl(mmart, eAct, nodeOwnerInviteAcceptM, withOfferText = true))
  }

  /** Сабмит формы подтверждения инвайта на управление ТЦ. */
  def nodeOwnerInviteAcceptFormSubmit(martId: String, eActId: String) = nodeOwnerInviteAcceptCommon(martId, eActId) { (eAct, mmart) => implicit request =>
    // Если юзер залогинен, то форму биндить не надо
    val formBinded = nodeOwnerInviteAcceptM.bindFromRequest()
    lazy val logPrefix = s"nodeOwnerInviteAcceptFormSubmit($martId, act=$eActId): "
    formBinded.fold(
      {formWithErrors =>
        debug(s"${logPrefix}Form bind failed: ${formatFormErrors(formWithErrors)}")
        NotAcceptable(invite.inviteAcceptFormTpl(mmart, eAct, formWithErrors, withOfferText = false))
      },
      {passwordOpt =>
        if (passwordOpt.isEmpty && !request.isAuth) {
          debug(s"${logPrefix}Password check failed. isEmpty=${passwordOpt.isEmpty} ;; request.isAuth=${request.isAuth}")
          val form1 = formBinded
            .withError("password.pw1", "error.required")
            .withError("password.pw2", "error.required")
          NotAcceptable(invite.inviteAcceptFormTpl(mmart, eAct, form1, withOfferText = false))
        } else {
          // Сначала удаляем запись об активации, убедившись что она не была удалена асинхронно.
          eAct.delete.flatMap { isDeleted =>
            val newPersonIdOptFut: Future[Option[String]] = if (!request.isAuth) {
              MPerson(lang = request2lang.code).save flatMap { personId =>
                EmailPwIdent.applyWithPw(email = eAct.email, personId=personId, password = passwordOpt.get, isVerified = true)
                  .save
                  .map { emailPwIdentId => Some(personId) }
              }
            } else {
              Future successful None
            }
            // Для обновления полей MMart требуется доступ к personId. Дожидаемся сохранения юзера...
            newPersonIdOptFut flatMap { personIdOpt =>
              val personId = (personIdOpt orElse request.pwOpt.map(_.personId)).get
              if (!(mmart.personIds contains personId)) {
                mmart.personIds += personId
              }
              mmart.save.map { _adnId =>
                Billing.maybeInitializeNodeBilling(_adnId)
                Redirect(routes.MarketLkAdn.showAdnNode(martId))
                  .flashing("success" -> "Регистрация завершена.")
                  .withSession(username -> personId)
              }
            }
          }
        }
      }
    )
  }

  private def nodeOwnerInviteAcceptCommon(adnId: String, eaId: String)(f: (EmailActivation, MAdnNode) => AbstractRequestWithPwOpt[AnyContent] => Future[Result]) = {
    MaybeAuth.async { implicit request =>
      bruteForceProtect flatMap { _ =>
        EmailActivation.getById(eaId) flatMap {
          case Some(eAct) if eAct.key == adnId =>
            MAdnNodeCache.getByIdCached(adnId) flatMap {
              case Some(mmart) =>
                f(eAct, mmart)(request)
              case None =>
                // should never occur
                error(s"nodeOwnerInviteAcceptCommon($adnId, eaId=$eaId): Mart not found, but code for mart exist. This should never occur.")
                NotFound(invite.inviteInvalidTpl("mart.not.found"))
            }

          case other =>
            // Неверный код активации или id магазина. Если None, то код скорее всего истёк. Либо кто-то брутфорсит.
            debug(s"nodeOwnerInviteAcceptCommon($adnId, eaId=$eaId): Invalid activation code (eaId): code not found. Expired?")
            // TODO Надо проверить, есть ли у юзера права на магазин, и если есть, то значит юзер дважды засабмиттил форму, и надо его сразу отредиректить в его магазин.
            // TODO Может и быть ситуация, что юзер всё ещё не залогинен, а второй сабмит уже тут. Нужно это тоже как-то обнаруживать. Например через временную сессионную куку из формы.
            warn(s"TODO I need to handle already activated requests!!!")
            NotFound(invite.inviteInvalidTpl("mart.activation.expired.or.invalid.code"))
        }
      }
    }
  }

}
