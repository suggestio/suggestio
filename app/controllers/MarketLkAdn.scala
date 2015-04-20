package controllers

import _root_.util.adn.NodesUtil
import _root_.util.async.AsyncUtil
import com.google.inject.Inject
import controllers.ident._
import models.msession.Keys
import models.usr.{MPerson, EmailActivation, EmailPwIdent}
import play.api.i18n.MessagesApi
import util.billing.Billing
import _root_.util.{FormUtil, PlayMacroLogsImpl}
import util.acl._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.lk.LkAdUtil
import scala.concurrent.Future
import views.html.lk.adn._
import views.html.lk.usr._
import io.suggest.ym.model.MAdnNode
import play.api.data.Form
import play.api.data.Forms._
import util.FormUtil._
import play.api.db.Database
import play.api.mvc.{AnyContent, Result}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 11:18
 * Description: Унифицированные части личного кабинета.
 */
class MarketLkAdn @Inject() (
  override val messagesApi: MessagesApi,
  db: Database
)
  extends SioController with PlayMacroLogsImpl with BruteForceProtectCtl with ChangePwAction
{

  import LOGGER._

  /** Список личных кабинетов юзера. */
  def lkList(fromAdnId: Option[String]) = IsAdnNodeAdminOptOrAuth(fromAdnId).async { implicit request =>
    val personId = request.pwOpt.get.personId
    val mnodesFut = MAdnNode.findByPersonId(personId)
    for {
      mnodes      <- mnodesFut
    } yield {
      Ok(views.html.lk.lkList(mnodes, request.mnodeOpt))
    }
  }

  /**
   * Отрендерить страницу ЛК какого-то узла рекламной сети. Экшен различает свои и чужие узлы.
   * @param adnId id узла.
   * @param povAdnIdOpt С точки зрения какого узла идёт просмотр указанного узла.
   *                    Выверенное значение это аргумента можно получить из request.povAdnNodeOpt.
   */
  def showAdnNode(adnId: String, povAdnIdOpt: Option[String]) = {
    AdnNodeAccessGet(adnId, povAdnIdOpt).async { implicit request =>
      import request.{adnNode, isMyNode}
      // Супервайзинг узла приводит к рендеру ещё одного виджета
      val slavesFut: Future[Seq[MAdnNode]] = if(isMyNode && request.adnNode.adn.isSupervisor) {
        MAdnNode.findBySupId(adnId)
      } else {
        Future successful Nil
      }

      // Делегированная модерация: собрать id узлов, которые делегировали adv-модерацию этому узлу
      // Сами узлы отображать вроде бы [пока] не нужно.
      val advDg2meIdsFut: Future[Seq[String]] = if (isMyNode) {
        MAdnNode.findIdsAdvDelegatedTo(adnId)
      } else {
        Future successful Nil
      }

      val isRcvr = adnNode.adn.isReceiver

      // Узнать, инфу по рекламодателям с учетом возможности делегированной к этому узлу модерации в качестве модератора.
      val advsForMeFut: Future[(Long, Seq[MAdnNode])] = if (isMyNode) {
        // Чтобы узнать список рекламодетелей, надо дождаться списка узлов, которые делегировали adv-работу этому узлу.
        advDg2meIdsFut flatMap { dgAdnIds =>
          var dgAdnIdsList = dgAdnIds.toList
          // Дописать в начало ещё текущей узел, если он также является рекламо-получателем.
          if (isRcvr)
            dgAdnIdsList ::= adnId
          // TODO Отрабатывать цепочное делегирование, когда узел делегирует дальше adv-права ещё какому-то узлу.
          val adnIdsSet = dgAdnIdsList.toSet
          // Собрать из sql-моделей инфу по размещениям.
          val syncResult = db.withConnection { implicit c =>
            // 2014.06.26: Скрывать бесплатные размещения, помеченные как isPartner.
            val okAdnIds = MAdvOk.findAllProducersForRcvrsPartner(adnIdsSet, isPartner = false)
            // Если adv-полномочия делегированы другому узлу, то не надо использовать ещё не принятые реквесты
            // для формирования списка текущих рекламодателей.
            val reqAdnIds: List[String] = if (adnNode.adn.advDelegate.isEmpty) {
              MAdvReq.findAllProducersForRcvrs(adnIdsSet)
            } else {
              Nil
            }
            val reqsCount = MAdvReq.countForRcvrs(adnIdsSet)
            (okAdnIds, reqAdnIds, reqsCount)
          }
          val (okAdnIds, reqAdnIds, reqsCount) = syncResult
          val advAdnIds = (okAdnIds ++ reqAdnIds).distinct
          MAdnNodeCache.multiGet(advAdnIds)
            .map { adns =>
              // Отсортировать рекламодателей по алфавиту.
              val adnsSorted = adns.sortBy(_.meta.name)
              reqsCount -> adnsSorted
            }
        }
      } else {
        Future successful (0L, Nil)
      }

      // Рендерить ли кнопку "рекламодатели"? Да, если ресивер либо есть ноды, делегировавшие сюда модерацию
      val showAdvsBtnFut: Future[Boolean] = {
        // Если это собственный ресивер, НЕ делегировавший adv-права другому узлу, то отображать кнопку.
        if (isMyNode && isRcvr && adnNode.adn.advDelegate.isEmpty) {
          Future successful true
        } else if (isMyNode) {
          // Возможно, это узел, которому делегировали adv-права модерации иные узлы.
          advDg2meIdsFut map { dgAdnIds =>
            dgAdnIds.nonEmpty
          }
        } else {
          // Скрывать кнопку "рекламодатели".
          Future successful false
        }
      }

      // Нужно ли отображать кнопку назад? Да, если у юзера есть ещё узлы.
      val showBackBtnFut = if (isMyNode) {
        // TODO Надо бы кеширование тут.
        MAdnNode.countByPersonId(request.pwOpt.get.personId)
          .map { _ >= 2L }
      } else {
        Future successful true
      }

      // 2014.sep.01 Понадобилось передавать welcomeAd в шаблон лк.
      val welcomeAdOptFut = adnNode.meta.welcomeAdId
        .fold(Future successful Option.empty[MWelcomeAd]) { MWelcomeAd.getById }

      // Дождаться всех фьючерсов и отрендерить результат.
      for {
        slaves          <- slavesFut
        (advReqsCount, advs) <- advsForMeFut
        showAdvsBtn     <- showAdvsBtnFut
        showBackBtn     <- showBackBtnFut
        welcomeAdOpt    <- welcomeAdOptFut
      } yield {
        Ok(adnNodeShowTpl(
          mnode         = adnNode,
          slaves        = slaves,
          isMyNode      = isMyNode,
          advertisers   = advs,
          povAdnIdOpt   = request.povAdnNodeOpt.flatMap(_.id),
          advReqsCount  = advReqsCount,
          showBackBtn   = showBackBtn,
          showAdvsBtn   = showAdvsBtn,
          welcomeAdOpt  = welcomeAdOpt
        ))
      }
    }
  }


  /**
   * Рендер страницы ЛК с рекламными карточками узла.
   * @param adnId id узла.
   * @param mode Режим фильтрации карточек.
   * @param newAdIdOpt Костыль: если была добавлена рекламная карточка, то она должна отобразится сразу,
   *                   независимо от refresh в индексе. Тут её id.
   * @param povAdnIdOpt id узла, с точки зрения которого идёт обзор узла.
   * @return 200 Ok + страница ЛК со списком карточек.
   */
  def showNodeAds(adnId: String, mode: MNodeAdsMode, newAdIdOpt: Option[String], povAdnIdOpt: Option[String]) = {
    AdnNodeAccessGet(adnId, povAdnIdOpt).async { implicit request =>
      import request.{adnNode, isMyNode}

      // Для узла нужно отобразить его рекламу.
      // TODO Добавить поддержку агрумента mode
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
            val adIds = db.withConnection { implicit c =>
              MAdv.findActualAdIdsBetweenNodes(MAdvModes.busyModes, adnId, rcvrId = povAdnNode.id.get)
            }
            MAd.multiGet(adIds)

          // pov-узел напрочь отсутствует. Нечего отображать.
          case None =>
            debug(s"showAdnNode($adnId, pov=$povAdnIdOpt): pov node is empty, no rcvr, no ads.")
            Future successful Nil
        }
      }

      // Собрать карту занятых размещением карточек.
      val ad2advMapFut = {
        request.myNodeId.fold(Future successful Map.empty[String, MAdvI]) { myAdnId =>
          Future.traverse(Seq(MAdvOk, MAdvReq)) { model =>
            Future {
              db.withConnection { implicit c =>
                model.findNotExpiredRelatedTo(myAdnId)
              }
            }(AsyncUtil.jdbcExecutionContext)
          } map { results =>
            advs2adIdMap(results : _*)
          }
        }
      }

      // Надо ли отображать кнопку "управление" под карточками? Да, если есть баланс и контракт.
      // Параллельности это не добавляет, но позволяет разблокировать play defaultContext от блокировки из-за JDBC.
      val canAdvFut: Future[Boolean] = {
        if (isMyNode && adnNode.adn.isProducer) {
          Future {
            db.withConnection { implicit c =>
              MBillContract.hasActiveForNode(adnId)  &&  MBillBalance.hasForNode(adnId)
            }
          }(AsyncUtil.jdbcExecutionContext)
        } else {
          Future successful false
        }
      }

      implicit val ctx = implicitly[Context]

      // 2015.apr.20: Вместо списка рекламных карточек надо передавать данные для рендера.
      val brArgssFut = madsFut flatMap { mads =>
        val dsOpt = ctx.deviceScreenOpt
        Future.traverse(mads) { mad =>
          LkAdUtil.tiledAdBrArgs(mad, dsOpt)
        }
      }

      // Рендер результата, когда все карточки будут собраны.
      for {
        brArgss   <- brArgssFut
        ad2advMap <- ad2advMapFut
        canAdv    <- canAdvFut
      } yield {
        val render = nodeAdsTpl(
          mnode       = adnNode,
          mode        = mode,
          mads        = brArgss,
          isMyNode    = isMyNode,
          povAdnIdOpt = request.povAdnNodeOpt.flatMap(_.id),
          canAdv      = canAdv,
          ad2advMap   = ad2advMap
        )(ctx)
        Ok(render)
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

  // Обработка инвайтов на управление узлом.
  /** Маппинг формы принятия инвайта. Содержит галочку для договора и опциональный пароль. */
  private def nodeOwnerInviteAcceptM = Form(tuple(
    "contractAgreed" -> boolean
      .verifying("error.contract.not.agreed", identity(_)),
    "password" -> optional(passwordWithConfirmM)
  ))

  /** Рендер страницы с формой подтверждения инвайта на управление ТЦ. */
  def nodeOwnerInviteAcceptForm(martId: String, eActId: String) = nodeOwnerInviteAcceptCommon(martId, eActId) {
    (eAct, adnNode) => implicit request =>
      Ok(invite.inviteAcceptFormTpl(adnNode, eAct, nodeOwnerInviteAcceptM, withOfferText = true))
  }

  /** Сабмит формы подтверждения инвайта на управление ТЦ. */
  def nodeOwnerInviteAcceptFormSubmit(adnId: String, eActId: String) = nodeOwnerInviteAcceptCommon(adnId, eActId) { (eAct, adnNode) => implicit request =>
    // Если юзер залогинен, то форму биндить не надо
    val formBinded = nodeOwnerInviteAcceptM.bindFromRequest()
    lazy val logPrefix = s"nodeOwnerInviteAcceptFormSubmit($adnId, act=$eActId): "
    formBinded.fold(
      {formWithErrors =>
        debug(s"${logPrefix}Form bind failed: ${formatFormErrors(formWithErrors)}")
        NotAcceptable(invite.inviteAcceptFormTpl(adnNode, eAct, formWithErrors, withOfferText = false))
      },
      {case (contractAgreed, passwordOpt) =>
        if (passwordOpt.isEmpty && !request.isAuth) {
          debug(s"${logPrefix}Password check failed. isEmpty=${passwordOpt.isEmpty} ;; request.isAuth=${request.isAuth}")
          val form1 = formBinded
            .withError("password.pw1", "error.required")
            .withError("password.pw2", "error.required")
          NotAcceptable(invite.inviteAcceptFormTpl(adnNode, eAct, form1, withOfferText = false))
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
              val nodeUpdateFut: Future[_] = if ( !(adnNode.personIds contains personId) ) {
                MAdnNode.tryUpdate(adnNode) { adnNode0 =>
                  adnNode0.copy(
                    personIds = adnNode0.personIds + personId
                  )
                }
              } else {
                Future successful Unit
              }
              nodeUpdateFut.map { _adnId =>
                Billing.maybeInitializeNodeBilling(adnId)
                Redirect(routes.MarketLkAdn.showAdnNode(adnId))
                  .flashing("success" -> "Регистрация завершена.")
                  .withSession(Keys.PersonId.name -> personId)
              }
            }
          }
        }
      }
    )
  }

  type F = (EmailActivation, MAdnNode) => AbstractRequestWithPwOpt[AnyContent] => Future[Result]

  private def nodeOwnerInviteAcceptCommon(adnId: String, eaId: String)(f: F) = {
    MaybeAuth.async { implicit request =>
      bruteForceProtected {
        EmailActivation.getById(eaId) flatMap {
          case Some(eAct) if eAct.key == adnId =>
            nodeOwnerInviteAcceptGo(adnId, eAct, f)

          case other =>
            // Неверный код активации или id магазина. Если None, то код скорее всего истёк. Либо кто-то брутфорсит.
            debug(s"nodeOwnerInviteAcceptCommon($adnId, eaId=$eaId): Invalid activation code (eaId): code not found. Expired?")
            // TODO Надо проверить, есть ли у юзера права на узел, и если есть, то значит юзер дважды засабмиттил форму, и надо его сразу отредиректить в его магазин.
            // TODO Может и быть ситуация, что юзер всё ещё не залогинен, а второй сабмит уже тут. Нужно это тоже как-то обнаруживать. Например через временную сессионную куку из формы.
            warn(s"TODO I need to handle already activated requests!!!")
            NotFound(invite.inviteInvalidTpl("mart.activation.expired.or.invalid.code"))
        }
      }
    }
  }

  /** Код одной из ветвей nodeOwnerInvitAcceptCommon. */
  private def nodeOwnerInviteAcceptGo(adnId: String, eAct: EmailActivation, f: F)(implicit request: AbstractRequestWithPwOpt[AnyContent]): Future[Result] = {
    MAdnNodeCache.getById(adnId) flatMap {
      case Some(adnNode) =>
        EmailPwIdent.getByEmail(eAct.email) flatMap {
          // email, на который выслан запрос, уже зареган в системе, но текущий юзер не подходит: тут у нас анонимус или левый юзер.
          case Some(epwIdent) if epwIdent.isVerified && !request.pwOpt.exists(_.personId == epwIdent.personId) =>
            debug(s"eAct has email = ${epwIdent.email}. This is personId[${epwIdent.personId}], but current pwOpt = ${request.pwOpt.map(_.personId)} :: Rdr user to login...")
            val result = IsAuth.onUnauthBase(request)
            if (request.isAuth) result.withNewSession else result

          // Юзер анонимус и такие email неизвестны системе, либо тут у нас текущий необходимый юзер.
          case _ =>
            f(eAct, adnNode)(request)
        }
      case None =>
        // should never occur
        error(s"nodeOwnerInviteAcceptCommon($adnId, eaId=${eAct.id.get}): ADN node not found, but act.code for node exist. This should never occur.")
        NotFound(invite.inviteInvalidTpl("adn.node.not.found"))
    }
  }


  /** Рендер страницы редактирования профиля пользователя в рамках ЛК узла. */
  def userProfileEdit(adnId: String, r: Option[String]) = IsAdnNodeAdminGet(adnId).apply { implicit request =>
    Ok(userProfileEditTpl(
      adnNode = request.adnNode,
      pf = ChangePw.changePasswordFormM,
      r = r
    ))
  }

  /** Сабмит формы смены пароля. */
  def changePasswordSubmit(adnId: String, r: Option[String]) = IsAdnNodeAdminPost(adnId).async { implicit request =>
    _changePasswordSubmit(r) { formWithErrors =>
      NotAcceptable(userProfileEditTpl(
        adnNode = request.adnNode,
        pf = formWithErrors,
        r = r
      ))
    }
  }


  import views.html.lk.adn.create._

  /** Маппинг формы создания нового узла (магазина). */
  private def createNodeFormM: UsrCreateNodeForm_t = {
    // TODO Добавить капчу.
    Form(
      "name" -> FormUtil.nameM
    )
  }

  /** Рендер страницы с формой создания нового узла (магазина). */
  def createNode = IsAuthGet { implicit request =>
    val form = createNodeFormM
    Ok(createTpl(form))
  }

  /** Сабмит формы создания нового узла для юзера. */
  def createNodeSubmit = IsAuthPost.async { implicit request =>
    createNodeFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("createNodeSubmit(): Failed to bind form:\n " + formatFormErrors(formWithErrors))
        NotAcceptable(createTpl(formWithErrors))
      },
      {nodeName =>
        NodesUtil.createUserNode(name = nodeName, personId = request.pwOpt.get.personId) map { adnNode =>
          Redirect( NodesUtil.userNodeCreatedRedirect(adnNode.id.get) )
            .flashing("success" -> "Создан новый магазин. Пожалуйста, заполните необходимые поля.")
        }
      }
    )
  }

}
