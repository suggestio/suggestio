package controllers

import io.suggest.color.MColorData
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.{EsModel, MEsNestedSearch}
import io.suggest.mbill2.m.item.MItems
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.edge.{MEdge, MEdgeInfo, MNodeEdges, MPredicates}
import io.suggest.n2.node.common.MNodeCommon
import io.suggest.n2.node.meta.{MBasicMeta, MMeta, MPersonMeta}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.sec.util.{Csrf, ScryptUtil}
import io.suggest.session.MSessionKeys
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.Inject
import models.mctx.Context
import models.mlk.MNodeShowArgs
import models.req.IReq
import models.usr._
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Result
import util.FormUtil
import util.FormUtil._
import util.acl._
import util.adn.NodesUtil
import util.img.{DynImgUtil, GalleryUtil, LogoUtil}
import util.showcase.ShowcaseUtil
import views.html.lk.adn._
import views.html.lk.adn.invite.inviteInvalidTpl
import views.html.lk.{lkList => lkListTpl}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 11:18
 * Description: Унифицированные части личного кабинета.
 */
final class MarketLkAdn @Inject() (
                                    sioControllerApi                    : SioControllerApi,
                                  )
  extends MacroLogsImpl
{

  import sioControllerApi._
  import mCommonDi.current.injector

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val nodesUtil = injector.instanceOf[NodesUtil]
  private lazy val scUtil = injector.instanceOf[ShowcaseUtil]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val logoUtil = injector.instanceOf[LogoUtil]
  private lazy val mItems = injector.instanceOf[MItems]
  private lazy val galleryUtil = injector.instanceOf[GalleryUtil]
  private lazy val isAuth = injector.instanceOf[IsAuth]
  private lazy val isNodeAdmin = injector.instanceOf[IsNodeAdmin]
  private lazy val isAdnNodeAdminOptOrAuth = injector.instanceOf[IsAdnNodeAdminOptOrAuth]
  private lazy val canUseNodeInvite = injector.instanceOf[CanUseNodeInvite]
  private lazy val dynImgUtil = injector.instanceOf[DynImgUtil]
  private lazy val scryptUtil = injector.instanceOf[ScryptUtil]
  private lazy val csrf = injector.instanceOf[Csrf]

  import mCommonDi.{slick, ec}


  /** Список личных кабинетов юзера. */
  def lkList(fromNodeId: Option[String]) = csrf.AddToken {
    isAdnNodeAdminOptOrAuth(fromNodeId, U.Lk).async { implicit request =>
      import esModel.api._
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

      lazy val logPrefix = s"showAdnNode($nodeId):"

      // Запустить обсчёт логотипа и галереи узла:
      val logoImgOpt = logoUtil.getLogoOfNode( request.mnode )
      val galleryFut = galleryUtil.galleryImgs( request.mnode )

      // Собрать статистику по подчинённым узлам:
      val ownedNodesStatsFut = mNodes.ntypeStats(
        new MNodeSearch {
          override val outEdges: MEsNestedSearch[Criteria] = {
            val cr = Criteria(
              predicates  = MPredicates.OwnedBy :: Nil,
              nodeIds     = nodeId :: Nil
            )
            MEsNestedSearch(
              clauses = cr :: Nil,
            )
          }
          override val nodeTypes = {
            MNodeTypes.adnTreeMemberTypes
              .filter(_ != MNodeTypes.Ad)
          }
        }
      )

      import slick.profile.api._

      // Собрать данные по размещениям узла на карте (lk-adn-map):
      val adnMapAdvsFut = slick.db.run {
        import mItems.MItemsTable._
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
          logoImgOpt  = logoImgOpt.toList,
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
          .map(_._2)
      }

      // Подготовить аргументы для рендера шаблона:
      val tplArgsFut = for {
        logoImgCallOpt        <- logoImgCallOptFut
        galleryCalls          <- galleryCallsFut
        ownedNodesStats       <- ownedNodesStatsFut
        adnMapAdvs            <- adnMapAdvsFut
      } yield {
        LOGGER.trace(s"$logPrefix adnMapAdvs[${adnMapAdvs.length}], subNodes=${ownedNodesStats}")
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

  private def eactNotFound(reason: String, mreq: IReq[_]): Future[Result] = {
    implicit val request = mreq
    NotFound( inviteInvalidTpl(reason) )
  }

  /** Рендер страницы с формой подтверждения инвайта на управление ТЦ. */
  def nodeOwnerInviteAcceptForm(qs: MEmailRecoverQs) = csrf.AddToken {
    canUseNodeInvite(qs)(eactNotFound) { implicit request =>
      val html = invite.inviteAcceptFormTpl(request.mnode, qs, nodeOwnerInviteAcceptM, withOfferText = true)
      Ok(html)
    }
  }

  /** Сабмит формы подтверждения инвайта на управление ТЦ. */
  def nodeOwnerInviteAcceptFormSubmit(qs: MEmailRecoverQs) = csrf.Check {
    canUseNodeInvite(qs)(eactNotFound).async { implicit request =>
      // Если юзер залогинен, то форму биндить не надо
      val formBinded = nodeOwnerInviteAcceptM.bindFromRequest()
      lazy val logPrefix = s"nodeOwnerInviteAcceptFormSubmit(${qs.nodeId.orNull} ${qs.email}): "
      formBinded.fold(
        {formWithErrors =>
          LOGGER.debug(s"${logPrefix}Form bind failed: ${formatFormErrors(formWithErrors)}")
          NotAcceptable(invite.inviteAcceptFormTpl(request.mnode, qs, formWithErrors, withOfferText = false))

        },
        {case (_, passwordOpt) =>
          val isAuth = request.user.isAuth
          if (passwordOpt.isEmpty && !isAuth) {
            LOGGER.debug(s"${logPrefix}Password check failed. isEmpty=${passwordOpt.isEmpty} ;; request.isAuth=$isAuth")
            val form1 = formBinded
              .withError("password.pw1", "error.required")
              .withError("password.pw2", "error.required")
            NotAcceptable(invite.inviteAcceptFormTpl(request.mnode, qs, form1, withOfferText = false))

          } else {
            import esModel.api._
            for {
              personIdOpt <- {
                if (!isAuth) {
                  val mperson0 = MNode(
                    common = MNodeCommon(
                      ntype = MNodeTypes.Person,
                      isDependent = false
                    ),
                    meta = MMeta(
                      basic = MBasicMeta(
                        nameOpt = Some( qs.email ),
                        langs = request.messages.lang.code :: Nil
                      ),
                      person = MPersonMeta(
                        emails = qs.email :: Nil
                      )
                    ),
                    edges = MNodeEdges(
                      out = {
                        val emailIdent = MEdge(
                          predicate = MPredicates.Ident.Email,
                          nodeIds   = Set.empty + qs.email,
                          info = MEdgeInfo(
                            flag = Some(true)
                          )
                        )
                        val pwIdent = MEdge(
                          predicate = MPredicates.Ident.Password,
                          info = MEdgeInfo(
                            textNi = Some( scryptUtil.mkHash(passwordOpt.get) )
                          )
                        )
                        val edges = emailIdent :: pwIdent :: Nil
                        MNodeEdges.edgesToMap1( edges )
                      }
                    )
                  )

                  // Сохранение данных.
                  for {
                    meta <- mNodes.save( mperson0 )
                  } yield {
                    meta.id
                  }
                } else {
                  Future.successful( None )
                }
              }

              personId = personIdOpt
                .orElse(request.user.personIdOpt)
                .get

              _ <- {
                val nodeOwnedByPersonId = {
                  request.mnode
                    .edges
                    .withPredicateIter( MPredicates.OwnedBy )
                    .exists(_.nodeIds.contains(personId))
                }
                if (!nodeOwnedByPersonId) {
                  val ownEdge = MEdge(
                    predicate = MPredicates.OwnedBy,
                    nodeIds   = Set.empty + personId,
                  )
                  mNodes.tryUpdate(request.mnode) {
                    MNode.edges
                      .composeLens( MNodeEdges.out )
                      .modify(_ :+ ownEdge)
                  }
                } else {
                  Future.successful(())
                }
              }

            } yield {
              Redirect(routes.LkAds.adsPage(request.mnode.id.get :: Nil))
                .flashing(FLASH.SUCCESS -> "Signup.finished")
                .withSession(MSessionKeys.PersonId.value -> personId)
            }
          }
        }
      )
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
  def createNode() = csrf.AddToken {
    isAuth() { implicit request =>
      val form = createNodeFormM
      Ok(createTpl(form))
    }
  }


  /** Сабмит формы создания нового узла для юзера. */
  def createNodeSubmit() = csrf.Check {
    isAuth().async { implicit request =>
      createNodeFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug("createNodeSubmit(): Failed to bind form:\n " + formatFormErrors(formWithErrors))
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
