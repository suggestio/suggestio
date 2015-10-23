package controllers

import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.MBasicMeta
import io.suggest.model.n2.node.meta.colors.MColorData
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import util.adn.NodesUtil
import util.async.AsyncUtil
import com.google.inject.Inject
import controllers.ident._
import io.suggest.event.SioNotifierStaticClientI
import models.im.logo.LogoUtil
import models.mlk.{MNodeAdsTplArgs, MNodeShowArgs}
import models.msession.Keys
import models.usr.EmailPwIdent
import org.elasticsearch.client.Client
import play.api.cache.CacheApi
import play.api.i18n.MessagesApi
import util.billing.Billing
import _root_.util.{FormUtil, PlayMacroLogsImpl}
import util.acl._
import models._
import util.ident.IdentUtil
import util.lk.LkAdUtil
import util.showcase.ShowcaseUtil
import scala.concurrent.{ExecutionContext, Future}
import views.html.lk.adn._
import views.html.lk.usr._
import views.html.lk.{lkList => lkListTpl}
import play.api.data.Form
import play.api.data.Forms._
import util.FormUtil._
import play.api.db.Database

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 11:18
 * Description: Унифицированные части личного кабинета.
 */
class MarketLkAdn @Inject() (
  override val messagesApi            : MessagesApi,
  nodesUtil                           : NodesUtil,
  lkAdUtil                            : LkAdUtil,
  db                                  : Database,
  scUtil                              : ShowcaseUtil,
  override val current                : play.api.Application,
  override val cache                  : CacheApi,
  override val mNodeCache             : MAdnNodeCache,
  override val identUtil              : IdentUtil,
  logoUtil                            : LogoUtil,
  billing                             : Billing,
  override val _contextFactory        : Context2Factory,
  override implicit val ec            : ExecutionContext,
  override implicit val esClient      : Client,
  override implicit val sn            : SioNotifierStaticClientI
)
  extends SioController
  with PlayMacroLogsImpl
  with BruteForceProtectCtl
  with ChangePwAction
  with NodeEactAcl
  with IsAdnNodeAdminOptOrAuth
  with AdnNodeAccess
  with IsAdnNodeAdmin
  with IsAuth
{

  import LOGGER._

  /** Список личных кабинетов юзера. */
  def lkList(fromAdnId: Option[String]) = IsAdnNodeAdminOptOrAuthGet(fromAdnId).async { implicit request =>
    val personId = request.pwOpt.get.personId
    val msearch = new MNodeSearchDfltImpl {
      override def outEdges = Seq(
        Criteria(
          nodeIds     = Seq(personId),
          predicates  = Seq(MPredicates.OwnedBy)
        )
      )
      override def limit      = 100
    }
    val mnodesFut = MNode.dynSearch(msearch)
    for {
      mnodes      <- mnodesFut
    } yield {
      Ok( lkListTpl(mnodes, request.mnodeOpt) )
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
      val logoOptFut = logoUtil.getLogoOfNode(adnNode)
      for {
        logoOpt <- logoOptFut
      } yield {
        val rargs = MNodeShowArgs(
          mnode         = adnNode,
          isMyNode      = isMyNode,
          povAdnIdOpt   = request.povAdnNodeOpt
            .flatMap(_.id),
          logoImgOpt    = logoOpt,
          bgColor       = colorCodeOrDflt(adnNode.meta.colors.bg, scUtil.SITE_BGCOLOR_DFLT),
          fgColor       = colorCodeOrDflt(adnNode.meta.colors.fg, scUtil.SITE_FGCOLOR_DFLT)
        )
        val html = adnNodeShowTpl( rargs )
        Ok(html)
      }
    }
  }

  private def colorCodeOrDflt(cdOpt: Option[MColorData], dflt: => String): String = {
    cdOpt.fold(dflt)(_.code)
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
              // Проверяем права доступа текущего узла на new-отображаемую карточку.
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
            MAd.multiGetRev(adIds)

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
      val canAdvFut: Future[Boolean] = {
        if (isMyNode && adnNode.extras.adn.exists(_.isProducer)) {
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
          lkAdUtil.tiledAdBrArgs(mad, dsOpt)
        }
      }

      // Рендер результата, когда все карточки будут собраны.
      for {
        brArgss   <- brArgssFut
        ad2advMap <- ad2advMapFut
        canAdv    <- canAdvFut
      } yield {
        val args = MNodeAdsTplArgs(
          mnode       = adnNode,
          mads        = brArgss,
          isMyNode    = isMyNode,
          povAdnIdOpt = request.povAdnNodeOpt.flatMap(_.id),
          canAdv      = canAdv,
          ad2advMap   = ad2advMap
        )
        val render = nodeAdsTpl(args)(ctx)
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
  def nodeOwnerInviteAcceptForm(adnId: String, eActId: String) = NodeEactGet(adnId, eActId) { implicit request =>
    Ok(invite.inviteAcceptFormTpl(request.mnode, request.eact, nodeOwnerInviteAcceptM, withOfferText = true))
  }

  /** Сабмит формы подтверждения инвайта на управление ТЦ. */
  def nodeOwnerInviteAcceptFormSubmit(adnId: String, eActId: String) = NodeEactPost(adnId, eActId).async { implicit request =>
    import request.{eact, mnode}
    // Если юзер залогинен, то форму биндить не надо
    val formBinded = nodeOwnerInviteAcceptM.bindFromRequest()
    lazy val logPrefix = s"nodeOwnerInviteAcceptFormSubmit($adnId, act=$eActId): "
    formBinded.fold(
      {formWithErrors =>
        debug(s"${logPrefix}Form bind failed: ${formatFormErrors(formWithErrors)}")
        NotAcceptable(invite.inviteAcceptFormTpl(mnode, eact, formWithErrors, withOfferText = false))

      }, { case (contractAgreed, passwordOpt) =>
        if (passwordOpt.isEmpty && !request.isAuth) {
          debug(s"${logPrefix}Password check failed. isEmpty=${passwordOpt.isEmpty} ;; request.isAuth=${request.isAuth}")
          val form1 = formBinded
            .withError("password.pw1", "error.required")
            .withError("password.pw2", "error.required")
          NotAcceptable(invite.inviteAcceptFormTpl(mnode, eact, form1, withOfferText = false))

        } else {
          // Сначала удаляем запись об активации, убедившись что она не была удалена асинхронно.
          eact.delete.flatMap { isDeleted =>
            val newPersonIdOptFut: Future[Option[String]] = if (!request.isAuth) {
              val mperson0 = MNode(
                common = MNodeCommon(
                  ntype = MNodeTypes.Person,
                  isDependent = false
                ),
                meta = MMeta(
                  basic = MBasicMeta(
                    nameOpt = Some(eact.email),
                    langs = List( request2lang.code )
                  ),
                  person = MPersonMeta(
                    emails = List(eact.email)
                  )
                )
              )
              mperson0.save flatMap { personId =>
                EmailPwIdent.applyWithPw(email = eact.email, personId = personId, password = passwordOpt.get, isVerified = true)
                  .save
                  .map { emailPwIdentId => Some(personId) }
              }
            } else {
              Future successful None
            }
            // Для обновления полей MMart требуется доступ к personId. Дожидаемся сохранения юзера...
            newPersonIdOptFut flatMap { personIdOpt =>
              val personId = (personIdOpt orElse request.pwOpt.map(_.personId)).get
              val nodeOwnedByPersonId = {
                mnode.edges
                  .withPredicateIter( MPredicates.OwnedBy )
                  .exists(_.nodeId == personId)
              }
              val nodeUpdateFut: Future[_] = if (!nodeOwnedByPersonId) {
                val ownEdge = MEdge(MPredicates.OwnedBy, personId)
                MNode.tryUpdate(mnode) { mnode0 =>
                  mnode0.copy(
                    edges = mnode0.edges.copy(
                      out = mnode0.edges.out + (ownEdge.toEmapKey -> ownEdge)
                    )
                  )
                }
              } else {
                Future successful Unit
              }
              nodeUpdateFut.map { _adnId =>
                billing.maybeInitializeNodeBilling(adnId)
                Redirect(routes.MarketLkAdn.showNodeAds(adnId))
                  .flashing(FLASH.SUCCESS -> "Signup.finished")
                  .withSession(Keys.PersonId.name -> personId)
              }
            }
          }
        }
      }
    )
  }


  /** Рендер страницы редактирования профиля пользователя в рамках ЛК узла. */
  def userProfileEdit(adnId: String, r: Option[String]) = IsAdnNodeAdminGet(adnId).apply { implicit request =>
    Ok {
      userProfileEditTpl(
        mnode = request.adnNode,
        pf    = ChangePw.changePasswordFormM,
        r     = r
      )
    }
  }

  /** Сабмит формы смены пароля. */
  def changePasswordSubmit(adnId: String, r: Option[String]) = IsAdnNodeAdminPost(adnId).async { implicit request =>
    _changePasswordSubmit(r) { formWithErrors =>
      NotAcceptable {
        userProfileEditTpl(
          mnode = request.adnNode,
          pf    = formWithErrors,
          r     = r
        )
      }
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
        val nodeFut = nodesUtil.createUserNode(
          name      = nodeName,
          personId  = request.pwOpt.get.personId
        )
        // Рендер HTTP-ответа.
        val respFut = nodeFut map { adnNode =>
          Redirect( nodesUtil.userNodeCreatedRedirect(adnNode.id.get) )
            .flashing(FLASH.SUCCESS -> "New.shop.created.fill.info")
        }
        // Вернуть HTTP-ответ.
        respFut
      }
    )
  }

}
