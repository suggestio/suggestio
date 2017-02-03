package controllers

import com.google.inject.Inject
import controllers.sysctl._
import controllers.sysctl.domain.SmDomains
import controllers.sysctl.invite.SmSendEmailInvite
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.MEsUuId
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.model.common.OptId
import io.suggest.model.n2.edge.MNodeEdges
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.MNodes
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.util.logs.MacroLogsImpl
import models._
import models.mctx.Context
import models.mproj.ICommonDi
import models.im.MImg3
import models.msc.MScAdsSearchQs
import models.msys._
import models.req.{INodeReq, IReq, MNodeReq}
import models.usr.{EmailActivation, EmailActivations, MPerson}
import org.elasticsearch.search.sort.SortOrder
import play.api.data._
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Call, Result}
import util.acl._
import util.adn.NodesUtil
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

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.02.14 17:21
 * Description: Тут управление компаниями, торговыми центрами и магазинами.
 */
class SysMarket @Inject() (
                            override val nodesUtil          : NodesUtil,
                            lkAdUtil                        : LkAdUtil,
                            advRcvrsUtil                    : AdvRcvrsUtil,
                            override val sysMarketUtil      : SysMarketUtil,
                            override val mailer             : IMailerWrapper,
                            override val n2NodesUtil        : N2NodesUtil,
                            override val sysAdRenderUtil    : SysAdRenderUtil,
                            emailActivations                : EmailActivations,
                            mPerson                         : MPerson,
                            mItems                          : MItems,
                            scAdSearchUtil                  : ScAdSearchUtil,
                            override val mNodes             : MNodes,
                            override val mCommonDi          : ICommonDi
)
  extends SioControllerImpl
  with MacroLogsImpl
  with SysNodeInstall
  with SmSendEmailInvite
  with SysAdRender
  with IsSuperuserMad
  with IsSuNode
  with IsSuperuser
  with IsSuperuserOr404
  with SmDomains
  with SysNodeEdges
{

  import LOGGER._
  import mCommonDi._
  import sysMarketUtil._

  /**
   * Корень /sys/ indexTpl.scala.html для системной панели.
   *
   * Изначально оно жило в ctl.Sys, который был замёржен в ctl.Application,
   * который тоже был упразднён 2015.dec.17.
   */
  def sysIndex = IsSuOr404Get { implicit request =>
    Ok( views.html.sys1.indexTpl() )
  }

  /** Корень /sys/marker/. Тут ссылки на дальнейшие страницы в рамках market. */
  def index = IsSuGet { implicit request =>
    Ok(marketIndexTpl())
  }

  /** Страница с унифицированным списком узлов рекламной сети в алфавитном порядке с делёжкой по memberType. */
  def adnNodesList(args: MSysNodeListArgs) = IsSuGet.async { implicit request =>
    // Запустить сбор статистики по типам N2-узлов:
    val ntypeStatsFut = mNodes.ntypeStats()

    // Собрать es-запрос согласно запросу, описанному в URL.
    val msearch = new MNodeSearchDfltImpl {
      override def nodeTypes    = args.ntypeOpt.toSeq
      override def shownTypeIds = args.stiOpt.toSeq.map(_.name)
      override def limit        = args.limit
      override def offset       = args.offset
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
      ntypeStatsFut flatMap { stats =>
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
        name      = ctx.messages("All"),
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


  /**
   * Страница отображения узла сети.
   *
   * @param nodeId id узла
   */
  def showAdnNode(nodeId: String) = IsSuNodeGet(nodeId).async { implicit request =>
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
          (ei.medge.predicate.strId,
            ei.medge.order.getOrElse(Int.MaxValue),
            nodeName)
        }
    }

    // Узнаём исходящие ребра.
    val outEdgesFut: Future[Seq[MNodeEdgeInfo]] = {
      val mnodesMapFut = mNodesCache.multiGetMap {
        mnode.edges
          .iterator
          .flatMap(_.nodeIds)
          .toSet
      }
      for (nmap <- mnodesMapFut) yield {
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
    }

    // Узнаём входящие ребра
    val inEdgesFut = {
      val msearch = new MNodeSearchDfltImpl {
        override def outEdges: Seq[ICriteria] = {
          val cr = Criteria(nodeIds = Seq(nodeId))
          Seq(cr)
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
                  mnodeEiths  = Seq(Right(mnode)),
                  edgeId      = None
                )
              }
          }
        _prepareEdgeInfos(iter)
      }
    }

    // Определить имена юзеров-владельцев. TODO Удалить, ибо во многом дублирует логику outEdges.
    val personNamesFut = {
      val ownerIds = mnode.edges
        .withPredicateIterIds( MPredicates.OwnedBy )
      Future.traverse( ownerIds ) { personId =>
        for {
          nameOpt <- mPerson.findUsernameCached(personId)
        } yield {
          val name = nameOpt.getOrElse(personId)
          personId -> name
        }
      }.map {
        _.toMap
      }
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


  /** Безвозвратное удаление узла рекламной сети. */
  def deleteAdnNodeSubmit(nodeId: String) = {
    val ab = IsSuNodePost(nodeId)
    ab.async { implicit request =>
      import request.mnode
      lazy val logPrefix = s"deleteAdnNodeSubmit($nodeId):"
      LOGGER.info(s"$logPrefix by user[${request.user.personIdOpt}] request. Deleting...")
      mNodes.deleteById(nodeId)
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
          case nse: NoSuchElementException =>
            warn(s"deleteAdnNodeSubmit($nodeId): Node not found. Anyway, resources re-erased.")
            ab.nodeNotFound(request)
        }
    }
  }


  /** Страница с формой создания нового узла. */
  def createAdnNode() = IsSuGet.async { implicit request =>
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

  /** Общий код create-node-экшенов с рендером страницы с формой создания узла. */
  private def createAdnNodeRender(nodeFormM: Form[MNode], ncpForm: Form[NodeCreateParams], rs: Status)
                                 (implicit request: IReq[_]): Future[Result] = {
    val html = createAdnNodeFormTpl(nodeFormM, ncpForm)
    Future.successful( rs(html) )
  }

  /** Сабмит формы создания нового узла. */
  def createAdnNodeSubmit() = IsSuPost.async { implicit request =>
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
            // Собрать новый инстанс узла.
            val resFut = for {
              // Если задан id создаваемого узла, убедится что этот id свободен:
              alreadyExistsOpt <- mNodes.maybeGetById( ncp.withId )
              if alreadyExistsOpt.isEmpty

              mnode1 = mnode0.copy(
                edges = mnode0.edges.copy(
                  out = {
                    val ownEdge = MEdge(
                      predicate = MPredicates.OwnedBy,
                      nodeIds   = request.user.personIdOpt.toSet
                    )
                    MNodeEdges.edgesToMap(ownEdge)
                  }
                ),
                // Возможно, id создаваемого документа уже задан.
                id          = ncp.withId
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
            resFut.recover { case ex: NoSuchElementException =>
              LOGGER.error(s"$logPrefix Node already exists: ${ncp.withId}", ex)
              Conflict(s"Node with ${ncp.withId.orNull} already exists.")
            }
          }
        )
      }
    )
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
        fut onFailure { case ex: Throwable =>
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
  def editAdnNode(adnId: String) = IsSuNodeGet(adnId).async { implicit request =>
    import request.mnode
    val formFilled = adnNodeFormM.fill(mnode)
    editAdnNodeBody(adnId, formFilled, Ok)
  }

  private def editAdnNodeBody(adnId: String, form: Form[MNode], rs: Status)
                             (implicit request: INodeReq[AnyContent]): Future[Result] = {
    val res = editAdnNodeFormTpl(request.mnode, form)
    Future.successful( rs(res) )
  }

  /** Самбит формы редактирования узла. */
  def editAdnNodeSubmit(adnId: String) = IsSuNodePost(adnId).async { implicit request =>
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


  // Инвайты на управление ТЦ

  /** Рендер страницы с формой инвайта (передачи прав на управление ТЦ). */
  def nodeOwnerInviteForm(adnId: String) = IsSuNodeGet(adnId).async { implicit request =>
    _nodeOwnerInviteFormSubmit(nodeOwnerInviteFormM, Ok)
  }

  private def _nodeOwnerInviteFormSubmit(form: Form[String], rs: Status)(implicit request: MNodeReq[_]): Future[Result] = {
    val eActsFut = emailActivations.findByKey( request.mnode.id.get )
    for (eActs <- eActsFut) yield {
      val html = nodeOwnerInvitesTpl(request.mnode, nodeOwnerInviteFormM, eActs)
      rs(html)
    }
  }

  /** Сабмит формы создания инвайта на управление ТЦ. */
  def nodeOwnerInviteFormSubmit(adnId: String) = IsSuNodePost(adnId).async { implicit request =>
    import request.mnode
    nodeOwnerInviteFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"martInviteFormSubmit($adnId): Failed to bind form: ${formWithErrors.errors}")
        _nodeOwnerInviteFormSubmit(formWithErrors, NotAcceptable)
      },
      {email1 =>
        val eAct = EmailActivation(email=email1, key = adnId)
        for (eActId <- emailActivations.save(eAct)) yield {
          val eact2 = eAct.copy(
            id = Some(eActId)
          )
          sendEmailInvite(eact2, mnode)
          // Письмо отправлено, вернуть админа назад в магазин
          Redirect(routes.SysMarket.showAdnNode(adnId))
            .flashing(FLASH.SUCCESS -> ("Письмо с приглашением отправлено на " + email1))
        }
      }
    )
  }


  // ======================================================================
  // отладка email-сообщений

  /** Отобразить html/txt email-сообщение активации без отправки куда-либо чего-либо. Нужно для отладки. */
  def showEmailInviteMsg(adnId: String) = IsSuNode(adnId) { implicit request =>
    import request.mnode
    val eAct = EmailActivation("test@test.com", id = Some("asdQE123_"))
    Ok( emailNodeOwnerInviteTpl(mnode, eAct) )
  }


  /** Отобразить технический список рекламных карточек узла. */
  def showAdnNodeAds(a: MScAdsSearchQs) = IsSuGet.async { implicit request =>

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
    val producerIds = __nodeIdsF(a.prodIdOpt)
    val rcvrIds     = __nodeIdsF(a.rcvrIdOpt)

    // Узнаём текущий узел на основе запроса. TODO Кривовато это как-то, может стоит через аргумент передавать?
    val adnNodeIdOpt = {
      producerIds
        .headOption
        .orElse {
          rcvrIds.headOption
        }
    }

    val adnNodeOptFut = FutureUtil.optFut2futOpt(adnNodeIdOpt)( mNodesCache.getById )

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
        .traverse(rcvrIds) { mNodesCache.getById }
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
          mNodesCache.multiGet(allRcvrIdsSet)
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


  /** Убрать указанную рекламную карточку из выдачи указанного ресивера. */
  def removeAdRcvr(adId: String, rcvrIdOpt: Option[String], r: Option[String]) = {
    IsSuMadPost(adId).async { implicit request =>
      // Запускаем спиливание ресивера для указанной рекламной карточки.
      val madSavedFut = advRcvrsUtil.depublishAdOn(request.mad, rcvrIdOpt.toSet)

      lazy val logPrefix = s"removeAdRcvr(ad[$adId]${rcvrIdOpt.fold("")(", rcvr[" + _ + "]")}): "
      // Радуемся в лог.
      rcvrIdOpt match {
        case Some(rcvrId) => info(logPrefix + "Starting removing for single rcvr...")
        case None         => warn(logPrefix + "Starting removing ALL rcvrs...")
      }

      // Начинаем асинхронно генерить ответ клиенту.
      val rdrToFut: Future[Result] = RdrBackOrFut(r) {
        val call = rcvrIdOpt.fold[Call] {
          n2NodesUtil.madProducerId(request.mad)
            .fold( routes.SysMarket.index() ) { prodId =>
              routes.SysMarket.showAdnNode(prodId)
            }
        } { rcvrId =>
          val adSearch = MScAdsSearchQs(
            rcvrIdOpt = Some(rcvrId)
          )
          routes.SysMarket.showAdnNodeAds(adSearch)
        }
        Future.successful( call )
      }

      // Дождаться завершения всех операций.
      for {
        rdr  <- rdrToFut
        mad2 <- madSavedFut
      } yield {
        // Вернуть редирект с результатом работы.
        rdr.flashing( FLASH.SUCCESS -> "Карточка убрана из выдачи." )
      }
    }
  }


  /** Отобразить email-уведомление об отключении указанной рекламы. */
  def showShopEmailAdDisableMsg(adId: String) = IsSuMad(adId).async { implicit request =>
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
            override def nodeTypes = Seq( MNodeTypes.AdnNode )
            override def withAdnRights = Seq( AdnRights.RECEIVER )
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
  def showNodeOwnerEmailInvite(adnId: String) = IsSuNode(adnId) { implicit request =>
    val eAct = EmailActivation("asdasd@kde.org", key=adnId, id = Some("123123asdasd_-123"))
    Ok( emailNodeOwnerInviteTpl(request.mnode, eAct) )
  }


  /**
   * Выдать sys-страницу относительно указанной карточки.
   *
   * @param adId id рекламной карточки.
   */
  def showAd(adId: String) = IsSuMadGet(adId).async { implicit request =>
    import request.mad

    // Определить узла-продьюсера
    val producerIdOpt = n2NodesUtil.madProducerId( mad )
    val producerOptFut = mNodesCache.maybeGetByIdCached( producerIdOpt )

    // Собрать инфу по картинкам.
    val imgs = {
      mad.edges
        .withPredicateIter( MPredicates.Bg, MPredicates.WcLogo, MPredicates.GalleryItem )
        .map { e =>
          MImgEdge(e, MImg3(e))
        }
        .toSeq
    }

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


  /** Вывести результат анализа ресиверов рекламной карточки. */
  def analyzeAdRcvrs(adId: String) = IsSuMadGet(adId).async { implicit request =>
    import request.mad
    val producerId = n2NodesUtil.madProducerId(mad).get
    val producerOptFut = mNodesCache.getById(producerId)

    val newRcvrsMapFut = for {
      producerOpt <- producerOptFut
      acc2 <- advRcvrsUtil.calculateReceiversFor(mad, producerOpt)
    } yield {
      // Нужна только карта ресиверов. Дроп всей остальной инфы...
      acc2.mad.edges.out
    }

    val rcvrsMap = n2NodesUtil.receiversMap(mad)

    // Достаём из кэша узлы.
    val nodesMapFut: Future[Map[String, MNode]] = {
      def _nodeIds(rcvrs: Receivers_t): Set[String] = {
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
        adns1       <- mNodesCache.multiGet(adnIds1)
        newRcvrsMap <- newRcvrsMapFut
        newAdns     <- {
          val newRcvrIds = _nodeIds(newRcvrsMap)
          mNodesCache.multiGet(newRcvrIds -- adnIds1)
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


  /** Пересчитать и сохранить ресиверы для указанной рекламной карточки. */
  def resetReceivers(adId: String, r: Option[String]) = IsSuMadPost(adId).async { implicit request =>
    for {
      _ <- advRcvrsUtil.resetReceiversFor(request.mad)
    } yield {
      // Когда всё будет сделано, отредиректить юзера назад на страницу ресиверов.
      RdrBackOr(r) { routes.SysMarket.analyzeAdRcvrs(adId) }
        .flashing(FLASH.SUCCESS -> s"Произведён сброс ресиверов узла-карточки.")
    }
  }


  /** Очистить полностью таблицу ресиверов. Бывает нужно для временного сокрытия карточки везде.
    * Это действие можно откатить через resetReceivers. */
  def cleanReceivers(adId: String, r: Option[String]) = IsSuMadPost(adId).async { implicit request =>
    for {
      _ <- advRcvrsUtil.cleanReceiverFor(request.mad)
    } yield {
      RdrBackOr(r) { routes.SysMarket.analyzeAdRcvrs(adId) }
        .flashing(FLASH.SUCCESS -> "Из узла вычищены все ребра ресиверов. Биллинг не затрагивался.")
    }
  }

}

