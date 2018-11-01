package controllers

import controllers.ident._
import io.suggest.color.MColorData
import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.item.MItems
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.edge.{MEdge, MPredicates}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta, MPersonMeta}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.sec.m.msession.Keys
import io.suggest.sec.util.ScryptUtil
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.{Inject, Singleton}
import models.UsrCreateNodeForm_t
import models.mctx.Context
import models.mlk.MNodeShowArgs
import models.mproj.ICommonDi
import models.req.{INodeReq, MReq}
import models.usr.{EmailActivations, EmailPwIdent, EmailPwIdents, MPersonIdents}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Result
import util.FormUtil
import util.FormUtil._
import util.acl._
import util.adn.NodesUtil
import util.ident.IdentUtil
import util.img.{DynImgUtil, GalleryUtil, LogoUtil}
import util.showcase.ShowcaseUtil
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
@Singleton
class MarketLkAdn @Inject() (
                              nodesUtil                           : NodesUtil,
                              scUtil                              : ShowcaseUtil,
                              mNodes                              : MNodes,
                              override val identUtil              : IdentUtil,
                              override val emailPwIdents          : EmailPwIdents,
                              override val mPersonIdents          : MPersonIdents,
                              emailActivations                    : EmailActivations,
                              logoUtil                            : LogoUtil,
                              mItems                              : MItems,
                              galleryUtil                         : GalleryUtil,
                              isAuth                              : IsAuth,
                              isNodeAdmin                         : IsNodeAdmin,
                              isAdnNodeAdminOptOrAuth             : IsAdnNodeAdminOptOrAuth,
                              nodeEact                            : NodeEact,
                              dynImgUtil                          : DynImgUtil,
                              override val scryptUtil             : ScryptUtil,
                              override val mCommonDi              : ICommonDi
                            )
  extends SioControllerImpl
  with MacroLogsImpl
  with ChangePwAction
{

  import LOGGER._
  import mCommonDi._
  import slick.profile.api._
  import mItems.MItemsTable._


  /** Список личных кабинетов юзера. */
  def lkList(fromAdnId: Option[String]) = csrf.AddToken {
    isAdnNodeAdminOptOrAuth(fromAdnId, U.Lk).async { implicit request =>
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
  }


  /**
   * Отрендерить страницу ЛК какого-то узла рекламной сети. Экшен различает свои и чужие узлы.
   *
   * @param nodeId id узла.
   */
  def showAdnNode(nodeId: String) = csrf.AddToken {
    isNodeAdmin(nodeId, U.Lk).async { implicit request =>

      // Запустить обсчёт логотипа и галереи узла:
      val logoImgOpt = logoUtil.getLogoOfNode( request.mnode )
      val galleryFut = galleryUtil.galleryImgs( request.mnode )

      // Собрать статистику по подчинённым узлам:
      val ownedNodesStatsFut = mNodes.ntypeStats(
        new MNodeSearchDfltImpl {
          override val outEdges: Seq[Criteria] = {
            val cr = Criteria(
              predicates  = MPredicates.OwnedBy :: Nil,
              nodeIds     = nodeId :: Nil
            )
            cr :: Nil
          }
          override val nodeTypes = {
            MNodeTypes.adnTreeMemberTypes
              .filter(_ != MNodeTypes.Ad)
          }
        }
      )

      // Собрать данные по размещениям узла на карте (lk-adn-map):
      val adnMapAdvsFut = slick.db.run {
        mItems.query
          .withNodeId( nodeId )
          .withTypes( MItemTypes.adnMapTypes )
          .itemsCurrentFor()
          .result
      }

      // Собрать карту media-хостов для картинок, которые надо будет рендерить:
      val mediaHostsMapFut = for {
        gallery         <- galleryFut
        mediaHostsMap0  <- nodesUtil.nodeMediaHostsMap(
          logoImgOpt  = logoImgOpt,
          gallery     = gallery
        )
      } yield {
        mediaHostsMap0
      }

      // Собрать ссылку на логотип узла.
      val logoImgCallOptFut = FutureUtil.optFut2futOpt( logoImgOpt ) { logoImg =>
        for {
          mediaHostsMap <- mediaHostsMapFut
        } yield {
          val logoImgCdnCall = dynImgUtil.distCdnImgCall(logoImg, mediaHostsMap)
          Some( logoImgCdnCall )
        }
      }

      val ctxFut = request.user.lkCtxDataFut.map { implicit lkCtxData =>
        implicitly[Context]
      }

      // Подготовить галеру к работе через CDN:
      val galleryCallsFut = for {
        galleryImgs   <- galleryFut
        ctx           <- ctxFut
        galleryCalls  <- galleryUtil.renderGalleryCdn(galleryImgs, mediaHostsMapFut)(ctx)
      } yield {
        galleryCalls
      }

      // Подготовить аргументы для рендера шаблона:
      val tplArgsFut = for {
        logoImgCallOpt        <- logoImgCallOptFut
        galleryCalls          <- galleryCallsFut
        ownedNodesStats       <- ownedNodesStatsFut
        adnMapAdvs            <- adnMapAdvsFut
      } yield {
        MNodeShowArgs(
          mnode               = request.mnode,
          bgColor             = colorCodeOrDflt(request.mnode.meta.colors.bg, scUtil.SITE_BGCOLOR_DFLT),
          fgColor             = colorCodeOrDflt(request.mnode.meta.colors.fg, scUtil.SITE_FGCOLOR_DFLT),
          gallery             = galleryCalls,
          logoImgCallOpt      = logoImgCallOpt,
          ownedNodesStats     = ownedNodesStats,
          adnMapAdvs          = adnMapAdvs,
        )
      }

      // Отрендерить и вернуть ответ:
      for {
        ctxData         <- request.user.lkCtxDataFut
        tplArgs         <- tplArgsFut
        ctx             <- ctxFut
      } yield {
        implicit val ctxData1 = ctxData
        val html = adnNodeShowTpl( tplArgs )(ctx)
        Ok(html)
      }
    }
  }

  private def colorCodeOrDflt(cdOpt: Option[MColorData], dflt: => String): String = {
    cdOpt.fold(dflt)(_.code)
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
  def nodeOwnerInviteAcceptForm(adnId: String, eActId: String) = csrf.AddToken {
    nodeEact(adnId, eActId)(eactNotFound) { implicit request =>
      Ok(invite.inviteAcceptFormTpl(request.mnode, request.eact, nodeOwnerInviteAcceptM, withOfferText = true))
    }
  }

  /** Сабмит формы подтверждения инвайта на управление ТЦ. */
  def nodeOwnerInviteAcceptFormSubmit(adnId: String, eActId: String) = csrf.Check {
    nodeEact(adnId, eActId)(eactNotFound).async { implicit request =>
      import request.{eact, mnode}
      // Если юзер залогинен, то форму биндить не надо
      val formBinded = nodeOwnerInviteAcceptM.bindFromRequest()
      lazy val logPrefix = s"nodeOwnerInviteAcceptFormSubmit($adnId, act=$eActId): "
      formBinded.fold(
        {formWithErrors =>
          debug(s"${logPrefix}Form bind failed: ${formatFormErrors(formWithErrors)}")
          NotAcceptable(invite.inviteAcceptFormTpl(mnode, eact, formWithErrors, withOfferText = false))

        }, { case (_, passwordOpt) =>
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
              _ <- emailActivations.deleteById(eActId)

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
                        langs = request.messages.lang.code :: Nil
                      ),
                      person = MPersonMeta(
                        emails = eact.email :: Nil
                      )
                    )
                  )

                  // Сохранение данных.
                  for {
                    personId <- mNodes.save(mperson0)
                    epwIdent = EmailPwIdent(
                      email       = eact.email,
                      personId    = personId,
                      pwHash      = scryptUtil.mkHash(passwordOpt.get),
                      isVerified  = true
                    )
                    _ <- emailPwIdents.save(epwIdent)
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
              Redirect(routes.LkAds.adsPage(adnId :: Nil))
                .flashing(FLASH.SUCCESS -> "Signup.finished")
                .withSession(Keys.PersonId.value -> personId)
            }
          }
        }
      )
    }
  }


  /** Рендер страницы редактирования профиля пользователя в рамках ЛК узла. */
  def userProfileEdit(adnId: String, r: Option[String]) = csrf.AddToken {
    isNodeAdmin(adnId, U.Lk).async { implicit request =>
      _userProfileEdit(ChangePw.changePasswordFormM, r, Ok)
    }
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
  def changePasswordSubmit(adnId: String, r: Option[String]) = csrf.Check {
    isNodeAdmin(adnId).async { implicit request =>
      _changePasswordSubmit(r) { formWithErrors =>
        _userProfileEdit(formWithErrors, r, NotAcceptable)
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
  def createNode = csrf.AddToken {
    isAuth() { implicit request =>
      val form = createNodeFormM
      Ok(createTpl(form))
    }
  }


  /** Сабмит формы создания нового узла для юзера. */
  def createNodeSubmit = csrf.Check {
    isAuth().async { implicit request =>
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

}
