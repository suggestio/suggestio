package controllers

import javax.inject.{Inject, Singleton}
import controllers.sysctl._
import controllers.sysctl.domain.SmDomains
import io.suggest.adn.MAdnRights
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.{EsModel, MEsUuId}
import io.suggest.i18n.MsgCodes
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.model.n2.edge.{MEdge, MNodeEdges, MPredicates}
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.{MNode, MNodeType, MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.primo.id.OptId
import io.suggest.sc.ads.MAdsSearchReq
import io.suggest.sc.sc3.{MScCommonQs, MScQs}
import io.suggest.sec.m.msession.Keys
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.Context
import models.mproj.ICommonDi
import models.msys._
import models.req.{INodeReq, IReq, IReqHdr, MNodeReq}
import models.usr.MEmailRecoverQs
import org.elasticsearch.search.sort.SortOrder
import play.api.data._
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Call, Result}
import util.acl._
import util.adn.NodesUtil
import util.adv.AdvUtil
import util.adv.direct.AdvRcvrsUtil
import util.lk.LkAdUtil
import util.mail.IMailerWrapper
import util.n2u.N2NodesUtil
import util.showcase.ScAdSearchUtil
import util.sys.SysMarketUtil
import views.html.lk.adn.invite.emailNodeOwnerInviteTpl
import views.html.lk.shop.ad.emailAdDisabledByMartTpl
import views.html.sys1.market._
import views.html.sys1.market.ad._
import views.html.sys1.market.adn._

import scala.concurrent.Future
import japgolly.univeq._
import models.madn.{AdnShownTypes, NodeDfltColors}
import util.ident.IdentUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.02.14 17:21
 * Description: Тут управление компаниями, торговыми центрами и магазинами.
 */
@Singleton
class SysMarket @Inject() (
                            override val esModel            : EsModel,
                            override val nodesUtil          : NodesUtil,
                            lkAdUtil                        : LkAdUtil,
                            advRcvrsUtil                    : AdvRcvrsUtil,
                            override val sysMarketUtil      : SysMarketUtil,
                            mailer                          : IMailerWrapper,
                            override val n2NodesUtil        : N2NodesUtil,
                            override val sysAdRenderUtil    : SysAdRenderUtil,
                            mItems                          : MItems,
                            override val advUtil            : AdvUtil,
                            override val isSuNodeEdge       : IsSuNodeEdge,
                            override val isSuNode           : IsSuNode,
                            override val isSuMad            : IsSuMad,
                            isSu                            : IsSu,
                            isSuOr404                       : IsSuOr404,
                            scAdSearchUtil                  : ScAdSearchUtil,
                            identUtil                       : IdentUtil,
                            override val mNodes             : MNodes,
                            override val mCommonDi          : ICommonDi
                          )
  extends SioControllerImpl
  with MacroLogsImpl
  with SysNodeInstall
  with SysAdRender
  with SmDomains
  with SysNodeEdges
{

  import LOGGER._
  import mCommonDi._
  import sysMarketUtil._
  import esModel.api._


  /**
   * Корень /sys/ indexTpl.scala.html для системной панели.
   *
   * Изначально оно жило в ctl.Sys, который был замёржен в ctl.Application,
   * который тоже был упразднён 2015.dec.17.
   */
  def sysIndex = csrf.AddToken {
    isSuOr404() { implicit request =>
      Ok( views.html.sys1.indexTpl() )
    }
  }


  /** Корень /sys/marker/. Тут ссылки на дальнейшие страницы в рамках market. */
  def index = csrf.AddToken {
    isSu() { implicit request =>
      Ok(marketIndexTpl())
    }
  }


  /** Страница с унифицированным списком узлов рекламной сети в алфавитном порядке с делёжкой по memberType. */
  def adnNodesList(args: MSysNodeListArgs) = csrf.AddToken {
    isSu().async { implicit request =>
      // Запустить сбор статистики по типам N2-узлов:
      val ntypeStatsFut = mNodes.ntypeStats()

      // Собрать es-запрос согласно запросу, описанному в URL.
      val msearch = new MNodeSearchDfltImpl {
        override def nodeTypes = args.ntypeOpt.toSeq
        override def shownTypeIds = args.stiOpt.toSeq.map(_.value)
        override def limit = args.limit
        override def offset = args.offset
        override def withNameSort = Some( SortOrder.ASC )
      }

      // Запустить поиск узлов для рендера:
      val mnodesFut = mNodes.dynSearch(msearch)

      // Кол-во вообще всех узлов.
      val allCountFut = for {
        stats <- ntypeStatsFut
      } yield {
        stats.valuesIterator.sum
      }

      // Считаем общее кол-во элементов указанного типа.
      val totalFut: Future[Long] = {
        ntypeStatsFut.flatMap { stats =>
          args.ntypeOpt.fold[Future[Long]] {
            allCountFut
          } { ntype =>
            val res = stats.getOrElse(ntype, 0L)
            Future.successful(res)
          }
        }
      }

      implicit val ctx = implicitly[Context]

      // Нужно сгенерить список кнопок-типов на основе статистики типов
      val ntypesFut = for {
        stats <- ntypeStatsFut
      } yield {
        // Полоска управления наверху подготавливается здесь
        MNodeTypes.values
          .iterator
          .flatMap { nt =>
            val ntype: MNodeType = nt
            for (count <- stats.get(ntype)) yield {
              MNodeTypeInfo(
                name      = ctx.messages( ntype.plural ),
                ntypeOpt  = Some(ntype),
                count     = count
              )
            }
          }
          .toStream
          .sortBy(_.name)
      }

      // Добавить ссылку "Все" в начало списка поисковых ссылок.
      val ntypes2Fut = for {
        allCount <- allCountFut
        ntypes0  <- ntypesFut
      } yield {
        val allNti = MNodeTypeInfo(
          name      = ctx.messages( MsgCodes.`All` ),
          ntypeOpt  = None,
          count     = allCount
        )
        allNti #:: ntypes0
      }

      // Объединение асинхронных результатов:
      for {
        mnodes <- mnodesFut
        ntypes <- ntypes2Fut
        total  <- totalFut
      } yield {
        val rargs = MSysNodeListTplArgs(
          mnodes = mnodes,
          ntypes = ntypes,
          args0  = args,
          total  = total
        )
        Ok( adnNodesListTpl(rargs)(ctx) )
      }
    }
  }


  /**
   * Страница отображения узла сети.
   *
   * @param nodeId id узла
   */
  def showAdnNode(nodeId: String) = csrf.AddToken {
    isSuNode(nodeId).async { implicit request =>
      import request.mnode

      def _prepareEdgeInfos(eis: TraversableOnce[MNodeEdgeInfo]): Seq[MNodeEdgeInfo] = {
        eis.toSeq
          .sortBy { ei =>
            // Собрать ключ для сортировки
            val nodeName = ei.mnodeEiths
              .iterator
              .map { _.fold [String] (identity, _.guessDisplayNameOrId.getOrElse("")) }
              .toStream
              .headOption
              .getOrElse("???")
            (ei.medge.predicate.value,
              ei.medge.order.getOrElse(Int.MaxValue),
              nodeName)
          }
      }

      val mnodesMapFut = mNodes.multiGetMapCache {
        mnode.edges
          .iterator
          .flatMap(_.nodeIds)
          .toSet
      }

      // Узнаём исходящие ребра.
      val outEdgesFut: Future[Seq[MNodeEdgeInfo]] = for {
        nmap <- mnodesMapFut
      } yield {
        val iter = for {
          (medge, index) <- mnode.edges.iterator.zipWithIndex
        } yield {
          val mnEiths = medge.nodeIds
            .iterator
            .map { nodeId =>
              nmap.get(nodeId)
                .toRight(nodeId)
            }
            .toSeq
          MNodeEdgeInfo(
            medge       = medge,
            mnodeEiths  = mnEiths,
            edgeId      = Some(index)
          )
        }
        _prepareEdgeInfos(iter)
      }

      // Узнаём входящие ребра
      val inEdgesFut = {
        val msearch = new MNodeSearchDfltImpl {
          override def outEdges: Seq[Criteria] = {
            val cr = Criteria(nodeIds = nodeId :: Nil)
            cr :: Nil
          }
          override def limit = 200
        }
        for {
          mnodes <- mNodes.dynSearch( msearch )
        } yield {
          val iter = mnodes.iterator
            .flatMap { mnode =>
              mnode.edges
                .withNodeId( nodeId )
                .map { medge =>
                  MNodeEdgeInfo(
                    medge       = medge,
                    mnodeEiths  = Right(mnode) :: Nil,
                    edgeId      = None
                  )
                }
            }
          _prepareEdgeInfos(iter)
        }
      }

      // Определить имена юзеров-владельцев.
      val personNamesFut = for (nmap <- mnodesMapFut) yield {
        val iter = for {
          nodeId <- mnode.edges
            .withPredicateIterIds( MPredicates.OwnedBy )
            .toSet[String]
            .iterator
          mnode <- nmap.get( nodeId ).iterator
        } yield {
          nodeId -> mnode.guessDisplayNameOrIdOrQuestions
        }
        iter.toMap
      }

      // Сгенерить асинхронный результат.
      for {
        outEdges    <- outEdgesFut
        inEdges     <- inEdgesFut
        personNames <- personNamesFut
      } yield {
        val args = MSysNodeShowTplArgs(
          mnode       = mnode,
          inEdges     = inEdges,
          outEdges    = outEdges,
          personNames = personNames
        )
        Ok( adnNodeShowTpl(args) )
      }
    }
  }


  /** Безвозвратное удаление узла рекламной сети. */
  def deleteAdnNodeSubmit(nodeId: String) = csrf.Check {
    val ab = isSuNode(nodeId)
    ab.async { implicit request =>
      import request.mnode
      lazy val logPrefix = s"deleteAdnNodeSubmit($nodeId):"
      LOGGER.info(s"$logPrefix by user[${request.user.personIdOpt}] request. Deleting...")
      mNodes
        .deleteById(nodeId)
        .filter(identity)
        .map { _ =>
          // Нужно перебрасывать на вкладку с узлами того же типа, что и удалённый.
          val args = MSysNodeListArgs(
            ntypeOpt = Some(mnode.common.ntype)
          )
          Redirect( routes.SysMarket.adnNodesList(args) )
            .flashing(FLASH.SUCCESS -> s"""Узел "${mnode.guessDisplayNameOrIdOrEmpty}" удалён.""")
        }
        .recoverWith {
          case _: NoSuchElementException =>
            val msg = "Node not found. Anyway, resources re-erased."
            warn(s"deleteAdnNodeSubmit($nodeId): $msg")
            errorHandler.onClientError(request, NOT_FOUND, msg)
        }
    }
  }


  /** Страница с формой создания нового узла. */
  def createAdnNode() = csrf.AddToken {
    isSu().async { implicit request =>
      // Генерим stub и втыкаем его в форму, чтобы меньше галочек ставить.
      // 2015.oct.21: Используем nodesUtil для сборки дефолтового инстанса.
      val dfltFormM = adnNodeFormM.fill(
        nodesUtil.userNodeInstance(
          nameOpt     = None,
          personIdOpt = request.user.personIdOpt
        )
      )

      val ncpForm = nodeCreateParamsFormM.fill( NodeCreateParams() )

      createAdnNodeRender(dfltFormM, ncpForm, Ok)
    }
  }

  /** Общий код create-node-экшенов с рендером страницы с формой создания узла. */
  private def createAdnNodeRender(nodeFormM: Form[MNode], ncpForm: Form[NodeCreateParams], rs: Status)
                                 (implicit request: IReq[_]): Future[Result] = {
    val html = createAdnNodeFormTpl(nodeFormM, ncpForm)
    Future.successful( rs(html) )
  }

  /** Сабмит формы создания нового узла. */
  def createAdnNodeSubmit() = csrf.Check {
    isSu().async { implicit request =>
      def logPrefix = s"createAdnNodeSubmit():"
      val ncpForm = nodeCreateParamsFormM.bindFromRequest()
      val nodeForm = adnNodeFormM.bindFromRequest()

      def __onError(formWithErrors: Form[_], formName: String): Future[Result] = {
        val renderFut = createAdnNodeRender(nodeForm, ncpForm, NotAcceptable)
        debug(s"$logPrefix Failed to bind $formName form: \n${formatFormErrors(formWithErrors)}")
        renderFut
      }

      nodeForm.fold(
        __onError(_, "node"),
        {mnode0 =>
          ncpForm.fold(
            __onError(_, "create"),
            {ncp =>
              val isAdnNode = mnode0.common.ntype ==* MNodeTypes.AdnNode
              // Собрать новый инстанс узла.
              val resFut = for {
              // Если задан id создаваемого узла, убедится что этот id свободен:
                alreadyExistsOpt <- mNodes.maybeGetById( ncp.withId )
                if alreadyExistsOpt.isEmpty

                mnode1 = mnode0.copy(
                  edges = mnode0.edges.copy(
                    out = {
                      var edgesAcc = List.empty[MEdge]

                      val personNodeIds = request.user.personIdOpt.toSet

                      // Если есть person-id, то сохранить связи до полей.
                      if (personNodeIds.nonEmpty) {
                        edgesAcc ::= MEdge(
                          // OwnedBy вешать НЕЛЬЗЯ, т.к. это вызывает недопонимание у суперюзеров (человеческий фактор).
                          // Например, цена начинает считаться по нулям у суперюзера даже при отключении соотв. галочки su free.
                          predicate = MPredicates.CreatedBy,
                          nodeIds   = personNodeIds
                        )

                        // Для ADN-узла надо выставлять ownedBy до супер-юзера.
                        if (isAdnNode) {
                          edgesAcc ::= MEdge(
                            predicate = MPredicates.OwnedBy,
                            nodeIds   = personNodeIds
                          )
                        }
                      }

                      MNodeEdges.edgesToMap( edgesAcc: _* )
                    }
                  ),
                  // Возможно, id создаваемого документа уже задан.
                  id          = ncp.withId,
                  // Если создаётся adn-узел, и цвета не заданы, то надо выставить рандомные цвета:
                  meta = if (isAdnNode && mnode0.meta.colors.adnColors.exists(_.isEmpty)) {
                    val colors2 = NodeDfltColors.getOneRandom().adnColors
                    LOGGER.trace(s"$logPrefix Resetting colors for created adn node: $colors2")
                    mnode0.meta
                      .withColors( colors2 )
                  } else {
                    mnode0.meta
                  }
                )
                nodeId <- mNodes.save(mnode1)

              } yield {
                // Инициализировать новосозданный узел согласно заданным параметрам.
                maybeInitializeNode(ncp, nodeId)

                val mnode2 = mnode1.withId( Some(nodeId) )
                // Отредиректить админа в созданный узел.
                Redirect(routes.SysMarket.showAdnNode(nodeId))
                  .flashing(FLASH.SUCCESS -> s"Создан узел сети: ${mnode2.guessDisplayNameOrIdOrEmpty}")
              }

              // Отрендерить ошибку совпадения id с существующим узлом...
              resFut.recoverWith { case ex: NoSuchElementException =>
                val msg = s"Node${ncp.withId.orNull} already exists"
                LOGGER.error(s"$logPrefix $msg", ex)
                errorHandler.onClientError(request, CONFLICT, msg)
              }
            }
          )
        }
      )
    }
  }

  /** При создании узла есть дополнительные отключаемые возможности по инициализации.
    * Тут фунцкия, отрабатывающая это дело. */
  private def maybeInitializeNode(ncp: NodeCreateParams, adnId: String)
                                 (implicit messages: Messages): Future[_] = {
    lazy val logPrefix = s"maybeInitializeNode($adnId):"

    // Все флаги отрабатываются аналогично, поэтому общий код вынесен за скобки.
    def f(flag: Boolean, errMsg: => String)(action: => Future[_]): Future[_] = {
      if (flag) {
        val fut: Future[_] = action
        for (ex <- fut.failed) {
          warn(errMsg, ex)
        }
        fut
      } else {
        Future.successful( None )
      }
    }
    val etgsFut = f(ncp.extTgsInit, s"$logPrefix Failed to create default targets") {
      nodesUtil.createExtDfltTargets(adnId)(messages)
    }
    val madsFut = f(ncp.withDfltMads, s"$logPrefix Failed to install default mads") {
      nodesUtil.installDfltMads(adnId)(messages)
    }
    etgsFut.flatMap { _ =>
      madsFut
    }
  }


  /** Страница с формой редактирования узла ADN. */
  def editAdnNode(adnId: String) = csrf.AddToken {
    isSuNode(adnId).async { implicit request =>
      import request.mnode
      val formFilled = adnNodeFormM.fill(mnode)
      editAdnNodeBody(adnId, formFilled, Ok)
    }
  }

  private def editAdnNodeBody(adnId: String, form: Form[MNode], rs: Status)
                             (implicit request: INodeReq[AnyContent]): Future[Result] = {
    val res = editAdnNodeFormTpl(request.mnode, form)
    Future.successful( rs(res) )
  }

  /** Самбит формы редактирования узла. */
  def editAdnNodeSubmit(adnId: String) = csrf.Check {
    isSuNode(adnId).async { implicit request =>
      import request.mnode
      val formBinded = adnNodeFormM.bindFromRequest()
      formBinded.fold(
        {formWithErrors =>
          debug(s"editAdnNodeSubmit($adnId): Failed to bind form: ${formatFormErrors(formWithErrors)}")
          editAdnNodeBody(adnId, formWithErrors, NotAcceptable)
        },
        {adnNode2 =>
          for {
            _ <- mNodes.tryUpdate(mnode) { updateAdnNode(_, adnNode2) }
          } yield {
            Redirect(routes.SysMarket.showAdnNode(adnId))
              .flashing(FLASH.SUCCESS -> "Changes.saved")
          }
        }
      )
    }
  }


  // Инвайты на управление ТЦ

  /** Рендер страницы с формой инвайта (передачи прав на управление ТЦ). */
  def nodeOwnerInviteForm(adnId: String) = csrf.AddToken {
    isSuNode(adnId).async { implicit request =>
      _nodeOwnerInviteFormSubmit(nodeOwnerInviteFormM, Ok)
    }
  }

  private def _nodeOwnerInviteFormSubmit(form: Form[String], rs: Status)(implicit request: MNodeReq[_]): Future[Result] = {
    val html = nodeOwnerInvitesTpl(request.mnode, nodeOwnerInviteFormM)
    rs(html)
  }

  /** Сабмит формы создания инвайта на управление ТЦ. */
  def nodeOwnerInviteFormSubmit(adnId: String) = csrf.Check {
    isSuNode(adnId).async { implicit request =>
      import request.mnode
      nodeOwnerInviteFormM.bindFromRequest().fold(
        {formWithErrors =>
          debug(s"martInviteFormSubmit($adnId): Failed to bind form: ${formWithErrors.errors}")
          _nodeOwnerInviteFormSubmit(formWithErrors, NotAcceptable)
        },
        {email1 =>
          val qs = MEmailRecoverQs(
            email   = email1,
            nodeId  = request.mnode.id,
          )
          sendEmailInvite(mnode, qs)
          // Письмо отправлено, вернуть админа назад в магазин
          Redirect(routes.SysMarket.showAdnNode(adnId))
            .flashing(FLASH.SUCCESS -> ("Письмо с приглашением отправлено на " + email1))
        }
      )
    }
  }


  /** Выслать письмо активации. */
  def sendEmailInvite(mnode: MNode, qs: MEmailRecoverQs)(implicit request: IReqHdr): Unit = {
    // Собираем и отправляем письмо адресату
    val msg = mailer.instance
    implicit val ctx = implicitly[Context]

    val ast = mnode.extras.adn
      .flatMap( _.shownTypeIdOpt )
      .flatMap( AdnShownTypes.withValueOpt )
      .getOrElse( AdnShownTypes.default )

    msg.setSubject(
      MsgCodes.`Suggest.io` + " | " +
      ctx.messages("Your") + " " +
      ctx.messages(ast.singular)
    )
    msg.setRecipients(qs.email)
    msg.setHtml {
      htmlCompressUtil.html4email {
        emailNodeOwnerInviteTpl(mnode, qs)(ctx)
      }
    }
    msg.send()
  }


  // ======================================================================
  // отладка email-сообщений

  /** Отобразить html/txt email-сообщение активации без отправки куда-либо чего-либо. Нужно для отладки. */
  def showEmailInviteMsg(adnId: String) = isSuNode(adnId) { implicit request =>
    import request.mnode
    val mockQs = MEmailRecoverQs(
      email = "test@test.com",
      nodeId = Some("asdQE123_")
    )
    Ok( emailNodeOwnerInviteTpl(mnode, mockQs) )
  }


  /** Отобразить технический список рекламных карточек узла. */
  def showAdnNodeAds(a: MScQs) = csrf.AddToken {
    isSu().async { implicit request =>

      // Ищем все рекламные карточки, подходящие под запрос.
      val msearchFut = scAdSearchUtil.qsArgs2nodeSearch(a)
      // TODO Нужна устойчивая сортировка.
      val madsFut = for {
        msearch <- msearchFut
        res     <- mNodes.dynSearch(msearch)
      } yield {
        res
      }

      val brArgssFut = madsFut.flatMap { mads =>
        Future.traverse(mads) { mad =>
          lkAdUtil.tiledAdBrArgs(mad)
        }
      }

      def __nodeIdsF(x: Option[MEsUuId]): Seq[String] = {
        x.iterator.map(_.id).toSeq
      }
      val producerIds = __nodeIdsF( a.search.prodId )
      val rcvrIds     = __nodeIdsF( a.search.rcvrId )

      // Узнаём текущий узел на основе запроса. TODO Кривовато это как-то, может стоит через аргумент передавать?
      val adnNodeIdOpt = {
        producerIds
          .headOption
          .orElse {
            rcvrIds.headOption
          }
      }

      val adnNodeOptFut = FutureUtil.optFut2futOpt(adnNodeIdOpt)( mNodes.getByIdCache )

      // Собираем карту размещений рекламных карточек.
      val ad2advMapFut: Future[Map[String, Seq[MItem]]] = {
        for {
          mads <- madsFut
          advs <- {
            import slick.profile.api._
            lazy val adIds = mads.flatMap(_.id)
            val q0 = {
              val statuses = MItemStatuses.advBusyIds.toSeq
              mItems.query
                .filter { i =>
                  (i.nodeId inSet adIds) && (i.statusStr inSet statuses)
                }
            }
            val items: Future[Seq[MItem]] = if (rcvrIds.nonEmpty) {
              // Ищем все размещения имеющихся карточек у запрошенных ресиверов.
              slick.db.run {
                q0.filter(_.rcvrIdOpt inSet rcvrIds).result
              }

            } else if (producerIds.nonEmpty) {
              // Ищем размещения карточек для продьюсера.
              slick.db.run {
                q0.result
              }

            } else {
              Future.successful(Nil)
            }
            items
          }
        } yield {
          advs.groupBy(_.nodeId)
        }
      }

      // Собираем ресиверов рекламных карточек.
      val rcvrsFut: Future[Map[String, Seq[MNode]]] = if (rcvrIds.nonEmpty) {
        // Используем только переданные ресиверы.
        Future
          .traverse(rcvrIds) { mNodes.getByIdCache }
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
        for {
          ad2advsMap  <- ad2advMapFut
          allRcvrs    <- {
            val allRcvrIdsSet = ad2advsMap.valuesIterator
              .flatten
              .flatMap(_.rcvrIdOpt)
              .toSet
            mNodes.multiGetCache(allRcvrIdsSet)
          }

        } yield {
          // Список ресиверов конвертим в карту ресиверов.
          val rcvrsMap = allRcvrs
            .iterator
            .map { rcvr =>
              rcvr.id.get -> rcvr
            }
            .toMap
          // Заменяем в исходной карте ad2advs списки adv на списки ресиверов.
          ad2advsMap.mapValues { advs =>
            advs.flatMap { adv =>
              adv.rcvrIdOpt
                .flatMap(rcvrsMap.get)
            }
          }
        }
      }

      // Планируем рендер страницы-результата, когда все данные будут собраны.
      for {
        brArgss       <- brArgssFut
        adnNodeOpt    <- adnNodeOptFut
        rcvrs         <- rcvrsFut
        ad2advMap     <- ad2advMapFut
        msearch       <- msearchFut
      } yield {
        val rargs = MShowNodeAdsTplArgs(brArgss, adnNodeOpt, rcvrs, a, ad2advMap, msearch)
        Ok( showAdnNodeAdsTpl(rargs) )
      }
    }
  }


  /** Убрать указанную рекламную карточку из выдачи указанного ресивера. */
  def removeAdRcvr(adId: String, rcvrIdOpt: Option[String], r: Option[String]) = csrf.Check {
    isSuMad(adId).async { implicit request =>
      // Запускаем спиливание ресивера для указанной рекламной карточки.
      val madSavedFut = advRcvrsUtil.depublishAdOn(request.mad, rcvrIdOpt.toSet)

      lazy val logPrefix = s"removeAdRcvr(ad[$adId]${rcvrIdOpt.fold("")(", rcvr[" + _ + "]")}): "
      // Радуемся в лог.
      rcvrIdOpt.fold {
        warn(logPrefix + "Starting removing ALL rcvrs...")
      } { _ =>
        info(logPrefix + "Starting removing for single rcvr...")
      }

      // Начинаем асинхронно генерить ответ клиенту.
      val rdrToFut: Future[Result] = RdrBackOrFut(r) {
        val call = rcvrIdOpt.fold[Call] {
          n2NodesUtil.madProducerId(request.mad)
            .fold( routes.SysMarket.index() ) { prodId =>
              routes.SysMarket.showAdnNode(prodId)
            }
        } { rcvrId =>
          val adSearch = MScQs(
            search = MAdsSearchReq(
              rcvrId = Some( rcvrId )
            ),
            common = MScCommonQs.empty
          )
          routes.SysMarket.showAdnNodeAds(adSearch)
        }
        Future.successful( call )
      }

      // Дождаться завершения всех операций.
      for {
        rdr  <- rdrToFut
        _    <- madSavedFut
      } yield {
        // Вернуть редирект с результатом работы.
        rdr.flashing( FLASH.SUCCESS -> "Карточка убрана из выдачи." )
      }
    }
  }


  /** Отобразить email-уведомление об отключении указанной рекламы. */
  def showShopEmailAdDisableMsg(adId: String) = isSuMad(adId).async { implicit request =>
    import request.mad

    // Получить ТЦ.
    val mmartFut = n2NodesUtil
      .receiverIds(mad)
      .toStream
      .headOption
      .fold [Future[Option[MNode]]] {
        // Тут хрень какая-то. Наугад выбирается случайный узел.
        mNodes.dynSearchOne {
          new MNodeSearchDfltImpl {
            override def nodeTypes = MNodeTypes.AdnNode :: Nil
            override def withAdnRights = MAdnRights.RECEIVER :: Nil
            override def limit = 1
          }
        }
      } {
        mNodes.getById(_)
      }

    for {
      mmartOpt <- mmartFut
    } yield {
      val reason = "Причина отключения ТЕСТ причина отключения 123123 ТЕСТ причина отключения."
      Ok( emailAdDisabledByMartTpl(mmartOpt.get, mad, reason) )
    }
  }

  /** Отрендериить тела email-сообщений инвайта передачи прав на ТЦ. */
  def showNodeOwnerEmailInvite(adnId: String) = isSuNode(adnId) { implicit request =>
    val qs = MEmailRecoverQs(
      email = "asdasd@suggest.io",
      nodeId = Some("123123asdasd_-123")
    )
    Ok( emailNodeOwnerInviteTpl(request.mnode, qs) )
  }


  /**
   * Выдать sys-страницу относительно указанной карточки.
   *
   * @param adId id рекламной карточки.
   */
  def showAd(adId: String) = csrf.AddToken {
    isSuMad(adId).async { implicit request =>
      import request.mad

      // Определить узла-продьюсера
      val producerIdOpt = n2NodesUtil.madProducerId( mad )
      val producerOptFut = mNodes.maybeGetByIdCached( producerIdOpt )

      // Собрать инфу по картинкам.
      // TODO Тут код наверное уже не актуален. Просто подправлен тут для совместимости.
      val imgs = List.empty[MImgEdge]
      /*
        mad.edges
          .withPredicateIter( MPredicates.JdContent.Image )
          .map { e =>
            MImgEdge(e, MImg3(e))
          }
          .toSeq
      */

      // Считаем кол-во ресиверов.
      val rcvrsCount = n2NodesUtil.receiverIds(mad)
        .toSet
        .size

      // Вернуть результат, когда всё будет готово.
      for {
        producerOpt <- producerOptFut
      } yield {
        val rargs = MShowAdTplArgs(mad, producerOpt, imgs, producerIdOpt, rcvrsCount)
        Ok( showAdTpl(rargs) )
      }
    }
  }


  /** Вывести результат анализа ресиверов рекламной карточки. */
  def analyzeAdRcvrs(adId: String) = csrf.AddToken {
    isSuMad(adId).async { implicit request =>
      import request.mad
      val producerId = n2NodesUtil.madProducerId(mad).get
      val producerOptFut = mNodes.getByIdCache(producerId)

      val newRcvrsMapFut = for {
        producerOpt <- producerOptFut
        acc2 <- advRcvrsUtil.calculateReceiversFor(mad, producerOpt)
      } yield {
        // Нужна только карта ресиверов. Дроп всей остальной инфы...
        acc2.mnode.edges.out
      }

      val rcvrsMap = n2NodesUtil.receiversMap(mad)

      // Достаём из кэша узлы.
      val nodesMapFut: Future[Map[String, MNode]] = {
        def _nodeIds(rcvrs: Seq[MEdge]): Set[String] = {
          if (rcvrs.nonEmpty) {
            rcvrs.iterator
              .map(_.nodeIds)
              .reduceLeft(_ ++ _)
          } else {
            Set.empty
          }
        }
        val adnIds1 = _nodeIds(rcvrsMap)
        for {
          adns1       <- mNodes.multiGetCache(adnIds1)
          newRcvrsMap <- newRcvrsMapFut
          newAdns     <- {
            val newRcvrIds = _nodeIds(newRcvrsMap)
            mNodes.multiGetCache(newRcvrIds -- adnIds1)
          }
        } yield {
          val iter = adns1.iterator ++ newAdns.iterator
          OptId.els2idMap[String, MNode](iter)
        }
      }

      // Узнать, совпадает ли рассчетная карта ресиверов с текущей.
      val rcvrsMapOkFut = for (newRcvrsMap <- newRcvrsMapFut) yield {
        advRcvrsUtil.isRcvrsMapEquals(newRcvrsMap, rcvrsMap)
      }

      for {
        newRcvrsMap <- newRcvrsMapFut
        producerOpt <- producerOptFut
        nodesMap    <- nodesMapFut
        rcvrsMapOk  <- rcvrsMapOkFut
      } yield {
        val rargs = MShowAdRcvrsTplArgs( mad, newRcvrsMap, nodesMap, producerOpt, rcvrsMap, rcvrsMapOk )
        Ok( showAdRcvrsTpl(rargs) )
      }
    }
  }


  /** Пересчитать и сохранить ресиверы для указанной рекламной карточки. */
  def resetReceivers(adId: String, r: Option[String]) = csrf.Check {
    isSuMad(adId).async { implicit request =>
      for {
        _ <- advRcvrsUtil.resetReceiversFor(request.mad)
      } yield {
        // Когда всё будет сделано, отредиректить юзера назад на страницу ресиверов.
        RdrBackOr(r) { routes.SysMarket.analyzeAdRcvrs(adId) }
          .flashing(FLASH.SUCCESS -> s"Произведён сброс ресиверов узла-карточки.")
      }
    }
  }


  /** Очистить полностью таблицу ресиверов. Бывает нужно для временного сокрытия карточки везде.
    * Это действие можно откатить через resetReceivers. */
  def cleanReceivers(adId: String, r: Option[String]) = csrf.Check {
    isSuMad(adId).async { implicit request =>
      for {
        _ <- advRcvrsUtil.cleanReceiverFor(request.mad)
      } yield {
        RdrBackOr(r) { routes.SysMarket.analyzeAdRcvrs(adId) }
          .flashing(FLASH.SUCCESS -> "Из узла вычищены все ребра ресиверов. Биллинг не затрагивался.")
      }
    }
  }


  /** Залогинится в указанный узел.
    *
    * @param nodeId id узла.
    * @return Редирект в ЛК с новой сессией.
    */
  def loginIntoNode(nodeIdU: MEsUuId) = csrf.Check {
    val nodeId = nodeIdU.id
    isSuNode(nodeId).async { implicit request =>
      LOGGER.info(s"loginIntoNode($nodeId): from personId#${request.user.personIdOpt.orNull}")
      for {
        call <- identUtil.redirectCallUserSomewhere( nodeId )
      } yield {
        Redirect( call )
          .withSession(Keys.PersonId.value -> nodeId)
      }
    }
  }

}

