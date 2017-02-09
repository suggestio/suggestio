package controllers

import com.google.inject.Inject
import controllers.ident._
import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.item.MItems
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.MNodes
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.MBasicMeta
import io.suggest.model.n2.node.meta.colors.MColorData
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.sec.m.msession.Keys
import io.suggest.sec.util.ScryptUtil
import io.suggest.util.logs.MacroLogsImpl
import models._
import models.mctx.Context
import models.mlk.{MNodeAdInfo, MNodeAdsMode, MNodeAdsTplArgs, MNodeShowArgs}
import models.mproj.ICommonDi
import models.req.{INodeReq, MReq}
import models.usr.{EmailActivations, EmailPwIdent, EmailPwIdents, MPersonIdents}
import org.elasticsearch.search.sort.SortOrder
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Result
import util.FormUtil._
import util.acl._
import util.adn.NodesUtil
import util.ident.IdentUtil
import util.img.{GalleryUtil, LogoUtil}
import util.lk.LkAdUtil
import util.showcase.ShowcaseUtil
import util.FormUtil
import views.html.lk.adn._
import views.html.lk.adn.invite.inviteInvalidTpl
import views.html.lk.usr._
import views.html.lk.{lkList => lkListTpl}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 11:18
 * Description: Унифицированные части личного кабинета.
 */
class MarketLkAdn @Inject() (
  nodesUtil                           : NodesUtil,
  lkAdUtil                            : LkAdUtil,
  scUtil                              : ShowcaseUtil,
  mItems                              : MItems,
  mNodes                              : MNodes,
  override val identUtil              : IdentUtil,
  override val emailPwIdents          : EmailPwIdents,
  override val mPersonIdents          : MPersonIdents,
  emailActivations                    : EmailActivations,
  logoUtil                            : LogoUtil,
  galleryUtil                         : GalleryUtil,
  isAuth                              : IsAuth,
  isAdnNodeAdmin                      : IsAdnNodeAdmin,
  isAdnNodeAdminOptOrAuth             : IsAdnNodeAdminOptOrAuth,
  nodeEact                            : NodeEact,
  override val scryptUtil             : ScryptUtil,
  override val mCommonDi              : ICommonDi
)
  extends SioController
  with MacroLogsImpl
  with BruteForceProtect
  with ChangePwAction
{

  import LOGGER._
  import mCommonDi._

  /** Список личных кабинетов юзера. */
  def lkList(fromAdnId: Option[String]) = isAdnNodeAdminOptOrAuth.Get(fromAdnId, U.Lk).async { implicit request =>
    val personId = request.user.personIdOpt.get

    val mnodesFut = mNodes.dynSearch(
      nodesUtil.personNodesSearch(personId)
    )

    for {
      ctxData     <- request.user.lkCtxDataFut
      mnodes      <- mnodesFut
    } yield {
      implicit val ctxData1 = ctxData
      Ok( lkListTpl(mnodes, request.mnodeOpt) )
    }
  }


  /**
   * Отрендерить страницу ЛК какого-то узла рекламной сети. Экшен различает свои и чужие узлы.
   *
   * @param nodeId id узла.
   */
  def showAdnNode(nodeId: String) = isAdnNodeAdmin.Get(nodeId, U.Lk).async { implicit request =>
    val mnode = request.mnode
    val logoOptFut = logoUtil.getLogoOfNode(mnode)
    val galleryFut = galleryUtil.galleryImgs( mnode )
    for {
      ctxData   <- request.user.lkCtxDataFut
      logoOpt   <- logoOptFut
      gallery   <- galleryFut
    } yield {
      val rargs = MNodeShowArgs(
        mnode         = mnode,
        logoImgOpt    = logoOpt,
        bgColor       = colorCodeOrDflt(mnode.meta.colors.bg, scUtil.SITE_BGCOLOR_DFLT),
        fgColor       = colorCodeOrDflt(mnode.meta.colors.fg, scUtil.SITE_FGCOLOR_DFLT),
        gallery       = gallery
      )

      implicit val ctxData1 = ctxData
      val html = adnNodeShowTpl( rargs )
      Ok(html)
    }
  }

  private def colorCodeOrDflt(cdOpt: Option[MColorData], dflt: => String): String = {
    cdOpt.fold(dflt)(_.code)
  }

  /**
   * Рендер страницы ЛК с рекламными карточками узла.
   *
   * @param adnId id узла.
   * @param mode Режим фильтрации карточек.
   * @param newAdIdOpt Костыль: если была добавлена рекламная карточка, то она должна отобразится сразу,
   *                   независимо от refresh в индексе. Тут её id.
   * @return 200 Ok + страница ЛК со списком карточек.
   */
  def showNodeAds(adnId: String, mode: MNodeAdsMode, newAdIdOpt: Option[String]) = {
    isAdnNodeAdmin.Get(adnId, U.Lk).async { implicit request =>
      import request.mnode

      // Для узла нужно отобразить его рекламу.
      // TODO Добавить поддержку агрумента mode
      val madsFut: Future[Seq[MNode]] = {
        val producedByCrs = {
          val cr = Criteria(
            nodeIds     = Seq( adnId ),
            predicates  = Seq( MPredicates.OwnedBy )
          )
          Seq(cr)
        }
        val adsSearch0 = new MNodeSearchDfltImpl {
          override def nodeTypes = Seq( MNodeTypes.Ad )
          override def outEdges  = producedByCrs
          override def limit     = 200
          // TODO Почему-то сортировка работает задом наперёд, должно быть DESC тут:
          override def withDateCreatedSort = Some(SortOrder.ASC)
        }
        // Это свой узел. Нужно в реалтайме найти рекламные карточки и проверить newAdIdOpt.
        val prodAdsFut = mNodes.dynSearchRt(adsSearch0)

        // Бывает, что добавлена новая карточка (но индекс ещё не сделал refresh). Нужно её найти и отобразить:
        val extAdOptFut = FutureUtil.optFut2futOpt(newAdIdOpt) { newAdId =>
          for {
            newAdOpt <- mNodes.getByIdType(newAdId, MNodeTypes.Ad)
            if newAdOpt.exists { newAd =>
              newAd.edges
                .withNodePred(adnId, MPredicates.OwnedBy)
                .nonEmpty
            }
          } yield {
            newAdOpt
          }
        }
        for {
          prodAds  <- prodAdsFut
          extAdOpt <- extAdOptFut
        } yield {
          // Если есть карточка в extAdOpt, то надо залить её в список карточек.
          extAdOpt.fold(prodAds) { extAd =>
            val i = prodAds.indexWhere(_.id == extAd.id)
            if ( i >= 0 ) {
              // Это возврат после edit. Заменить существующую карточку.
              prodAds.updated(i, extAd)
            } else {
              // Это возврат после create. Дописать карточку вначало.
              extAdOpt.get #:: prodAds.toStream
            }
          }
        }

      }

      // Нужно бегло собрать инфу о текущих размещениях карточек в базе без подробностей.
      val itemsInfosFut = for {
        mads <- madsFut
        itemsInfos <- {
          slick.db.run {
            mItems.findStatusesForAds(
              adIds     = mads.flatMap(_.id),
              statuses  = MNodeAdInfo.statusesSupported.toSeq
            )
          }
        }
      } yield {
        itemsInfos
          .iterator
          .map { i => (i.nodeId, i) }
          .toMap
      }

      // Надо ли отображать кнопку "управление" под карточками? Да, если узел продьюсер.
      val canAdvFut: Future[Boolean] = {
        val canAdv = mnode.extras.adn.exists(_.isProducer)
        Future.successful( canAdv )
      }

      val ctxFut = request.user.lkCtxDataFut.map { implicit ctxData =>
        implicitly[Context]
      }

      val mnaisFut = for {
        mads <- madsFut
        ctx  <- ctxFut
        // Собираем данные для рендера карточек
        res  <- {
          val dsOpt = ctx.deviceScreenOpt
          Future.traverse(mads) { mad =>
            lkAdUtil.tiledAdBrArgs(mad, dsOpt)
          }
        }
        itemsInfos <- itemsInfosFut
      } yield {
        // Заворачиваем данные для рендера и по размещениям в контейнеры.
        for (brArgs <- res) yield {
          val iiOpt = brArgs.mad.id
            .flatMap(itemsInfos.get)
          val iStatuses = iiOpt
            .map(_.statuses)
            .getOrElse(Set.empty)
          MNodeAdInfo(brArgs, iStatuses)
        }
      }

      // Рендер результата, когда все карточки будут собраны.
      for {
        mnais     <- mnaisFut
        canAdv    <- canAdvFut
        ctx       <- ctxFut
      } yield {
        val args = MNodeAdsTplArgs(
          mnode   = mnode,
          mads    = mnais,
          canAdv  = canAdv
        )
        val render = nodeAdsTpl(args)(ctx)
        Ok(render)
      }
    }
  }


  // Обработка инвайтов на управление узлом.
  /** Маппинг формы принятия инвайта. Содержит галочку для договора и опциональный пароль. */
  private def nodeOwnerInviteAcceptM = Form(tuple(
    "contractAgreed" -> boolean
      .verifying("error.contract.not.agreed", identity(_)),
    "password" -> optional(passwordWithConfirmM)
  ))

  private def eactNotFound(reason: String, mreq: MReq[_]): Future[Result] = {
    implicit val request = mreq
    NotFound( inviteInvalidTpl(reason) )
  }

  /** Рендер страницы с формой подтверждения инвайта на управление ТЦ. */
  def nodeOwnerInviteAcceptForm(adnId: String, eActId: String) = nodeEact.Get(adnId, eActId)(eactNotFound) { implicit request =>
    Ok(invite.inviteAcceptFormTpl(request.mnode, request.eact, nodeOwnerInviteAcceptM, withOfferText = true))
  }

  /** Сабмит формы подтверждения инвайта на управление ТЦ. */
  def nodeOwnerInviteAcceptFormSubmit(adnId: String, eActId: String) = nodeEact.Post(adnId, eActId)(eactNotFound).async { implicit request =>
    import request.{eact, mnode}
    // Если юзер залогинен, то форму биндить не надо
    val formBinded = nodeOwnerInviteAcceptM.bindFromRequest()
    lazy val logPrefix = s"nodeOwnerInviteAcceptFormSubmit($adnId, act=$eActId): "
    formBinded.fold(
      {formWithErrors =>
        debug(s"${logPrefix}Form bind failed: ${formatFormErrors(formWithErrors)}")
        NotAcceptable(invite.inviteAcceptFormTpl(mnode, eact, formWithErrors, withOfferText = false))

      }, { case (contractAgreed, passwordOpt) =>
        val isAuth = request.user.isAuth
        if (passwordOpt.isEmpty && !isAuth) {
          debug(s"${logPrefix}Password check failed. isEmpty=${passwordOpt.isEmpty} ;; request.isAuth=$isAuth")
          val form1 = formBinded
            .withError("password.pw1", "error.required")
            .withError("password.pw2", "error.required")
          NotAcceptable(invite.inviteAcceptFormTpl(mnode, eact, form1, withOfferText = false))

        } else {
          // Сначала удаляем запись об активации, убедившись что она не была удалена асинхронно.
          for {
            isDeleted <- emailActivations.deleteById(eActId)

            personIdOpt <- {
              if (!isAuth) {
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

                // Сохранение данных.
                for {
                  personId        <- mNodes.save(mperson0)
                  emailPwIdentId  <- {
                    val epwIdent = EmailPwIdent(
                      email       = eact.email,
                      personId    = personId,
                      pwHash      = scryptUtil.mkHash(passwordOpt.get),
                      isVerified  = true
                    )
                    emailPwIdents.save(epwIdent)
                  }
                } yield {
                  Some(personId)
                }
              } else {
                Future.successful( None )
              }
            }

            personId = personIdOpt.orElse(request.user.personIdOpt).get

            _ <- {
              val nodeOwnedByPersonId = {
                mnode.edges
                  .withPredicateIter( MPredicates.OwnedBy )
                  .exists(_.nodeIds.contains(personId))
              }
              if (!nodeOwnedByPersonId) {
                val ownEdge = MEdge(
                  predicate = MPredicates.OwnedBy,
                  nodeIds   = Set(personId)
                )
                mNodes.tryUpdate(mnode) { mnode0 =>
                  mnode0.copy(
                    edges = mnode0.edges.copy(
                      out = mnode0.edges.out ++ Seq(ownEdge)
                    )
                  )
                }
              } else {
                Future.successful(Unit)
              }
            }

          } yield {
            Redirect(routes.MarketLkAdn.showNodeAds(adnId))
              .flashing(FLASH.SUCCESS -> "Signup.finished")
              .withSession(Keys.PersonId.name -> personId)
          }
        }
      }
    )
  }


  /** Рендер страницы редактирования профиля пользователя в рамках ЛК узла. */
  def userProfileEdit(adnId: String, r: Option[String]) = isAdnNodeAdmin.Get(adnId, U.Lk).async { implicit request =>
    _userProfileEdit(ChangePw.changePasswordFormM, r, Ok)
  }

  private def _userProfileEdit(form: Form[(String, String)], r: Option[String], rs: Status)
                              (implicit request: INodeReq[_]): Future[Result] = {
    request.user.lkCtxDataFut.map { implicit ctxData =>
      val html = userProfileEditTpl(
        mnode = request.mnode,
        pf    = form,
        r     = r
      )
      rs(html)
    }
  }

  /** Сабмит формы смены пароля. */
  def changePasswordSubmit(adnId: String, r: Option[String]) = isAdnNodeAdmin.Post(adnId).async { implicit request =>
    _changePasswordSubmit(r) { formWithErrors =>
      _userProfileEdit(formWithErrors, r, NotAcceptable)
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
  def createNode = isAuth.Get { implicit request =>
    val form = createNodeFormM
    Ok(createTpl(form))
  }

  /** Сабмит формы создания нового узла для юзера. */
  def createNodeSubmit = isAuth.Post.async { implicit request =>
    createNodeFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("createNodeSubmit(): Failed to bind form:\n " + formatFormErrors(formWithErrors))
        NotAcceptable(createTpl(formWithErrors))
      },
      {nodeName =>
        val nodeFut = nodesUtil.createUserNode(
          name      = nodeName,
          personId  = request.user.personIdOpt.get
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
