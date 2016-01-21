package controllers

import com.google.inject.Inject
import controllers.sysctl._
import io.suggest.common.fut.FutureUtil
import io.suggest.model.n2.edge.MNodeEdges
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import models._
import models.adv.{MAdvI, MAdvOk, MAdvReq}
import models.mctx.Context
import models.mproj.ICommonDi
import models.im.MImgs3
import models.msys._
import models.req.{INodeReq, IReq}
import models.usr.{EmailActivation, MPerson}
import org.elasticsearch.search.sort.SortOrder
import play.api.data._
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Call, Result}
import play.twirl.api.Html
import util.PlayMacroLogsImpl
import util.acl._
import util.adn.NodesUtil
import util.adv.AdvUtil
import util.async.AsyncUtil
import util.lk.LkAdUtil
import util.mail.IMailerWrapper
import util.n2u.N2NodesUtil
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
  advUtil                         : AdvUtil,
  sysMarketUtil                   : SysMarketUtil,
  override val mailer             : IMailerWrapper,
  override val n2NodesUtil        : N2NodesUtil,
  override val sysAdRenderUtil    : SysAdRenderUtil,
  mPerson                         : MPerson,
  override val mCommonDi          : ICommonDi,
  mImgs3                          : MImgs3
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with SysNodeInstall
  with SmSendEmailInvite
  with SysAdRender
  with IsSuperuserMad
  with IsSuNode
  with IsSuperuser
  with IsSuperuserOr404
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
  def sysIndex = IsSuperuserOr404 { implicit request =>
    Ok( views.html.sys1.indexTpl() )
  }

  /** Корень /sys/marker/. Тут ссылки на дальнейшие страницы в рамках market. */
  def index = IsSuperuser { implicit request =>
    Ok(marketIndexTpl())
  }

  /** Страница с унифицированным списком узлов рекламной сети в алфавитном порядке с делёжкой по memberType. */
  def adnNodesList(args: MSysNodeListArgs) = IsSuperuser.async { implicit request =>
    // Запустить сбор статистики по типам N2-узлов:
    val ntypeStatsFut = MNode.ntypeStats()

    // Собрать es-запрос согласно запросу, описанному в URL.
    val msearch = new MNodeSearchDfltImpl {
      override def nodeTypes    = args.ntypeOpt.toSeq
      override def shownTypeIds = args.stiOpt.toSeq.map(_.name)
      override def limit        = args.limit
      override def offset       = args.offset
      override def withNameSort = Some( SortOrder.ASC )
    }

    // Запустить поиск узлов для рендера:
    val mnodesFut = MNode.dynSearch(msearch)

    // Кол-во вообще всех узлов.
    val allCountFut = for {
      stats <- ntypeStatsFut
    } yield {
      stats.valuesIterator.sum
    }

    // Считаем общее кол-во элементов указанного типа.
    val totalFut: Future[Long] = {
      ntypeStatsFut flatMap { stats =>
        args.ntypeOpt match {
          case Some(ntype) =>
            val res = stats.getOrElse(ntype, 0L)
            Future successful res
          case None =>
            allCountFut
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
          stats.get(ntype).map { count =>
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
   * @param nodeId id узла
   */
  def showAdnNode(nodeId: String) = IsSuNode(nodeId).async { implicit request =>
    import request.mnode

    def _prepareEdgeInfos(eis: TraversableOnce[MNodeEdgeInfo]): Seq[MNodeEdgeInfo] = {
      eis.toSeq
        .sortBy { ei =>
          // Собрать ключ для сортировки
          val nodeName = ei.mnodeEith
            .fold [String] (identity, _.guessDisplayNameOrId.getOrElse(""))
          (ei.medge.predicate.strId,
            ei.medge.order.getOrElse(Int.MaxValue),
            nodeName)
        }
    }

    // Узнаём исходящие ребра.
    val outEdgesFut: Future[Seq[MNodeEdgeInfo]] = {
      MNode.multiGetMap {
        mnode.edges.out
          .valuesIterator
          .map(_.nodeId)
      } map { nmap =>
        val iter = mnode.edges.out
          .valuesIterator
          .map { medge =>
            val nodeId = medge.nodeId
            val mnodeOpt = nmap.get(nodeId)
            val mnodeEith = mnodeOpt.toRight( nodeId )
            MNodeEdgeInfo(medge, mnodeEith)
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
        mnodes <- MNode.dynSearch( msearch )
      } yield {
        val iter = mnodes.iterator
          .flatMap { mnode =>
            mnode.edges
              .withNodeId( nodeId )
              .map { medge =>
                MNodeEdgeInfo(medge, Right(mnode))
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
        mPerson.findUsernameCached(personId)
          .map { nameOpt =>
            val name = nameOpt.getOrElse(personId)
            personId -> name
          }
      } map {
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
  def deleteAdnNodeSubmit(adnId: String) = {
    val ab = IsSuNodePost(adnId)
    ab.async { implicit request =>
      import request.mnode
      mnode
        .delete
        .filter(identity)
        .map { _ =>
          Redirect( routes.SysMarket.adnNodesList() )
            .flashing(FLASH.SUCCESS -> "Узел ADN удалён.")
        }
        .recoverWith {
          case nse: NoSuchElementException =>
            warn(s"deleteAdnNodeSubmit($adnId): Node not found. Anyway, resources re-erased.")
            ab.nodeNotFound(request)
        }
    }
  }


  private def createAdnNodeRender(nodeFormM: Form[MNode], ncpForm: Form[NodeCreateParams])
                                 (implicit request: IReq[_]): Future[Html] = {
    val html = createAdnNodeFormTpl(nodeFormM, ncpForm)
    Future successful html
  }

  private def nodeCreateParamsFormM = Form(NodeCreateParams.mappingM)

  /** Страница с формой создания нового узла. */
  def createAdnNode() = IsSuperuser.async { implicit request =>
    // Генерим stub и втыкаем его в форму, чтобы меньше галочек ставить.
    // 2015.oct.21: Используем nodesUtil для сборки дефолтового инстанса.
    val dfltFormM = adnNodeFormM.fill(
      nodesUtil.userNodeInstance("", personId = request.user.personIdOpt.get)
    )
    val ncpForm = nodeCreateParamsFormM fill NodeCreateParams()
    createAdnNodeRender(dfltFormM, ncpForm)
      .map { Ok(_) }
  }

  /** Сабмит формы создания нового узла. */
  def createAdnNodeSubmit() = IsSuperuser.async { implicit request =>
    val ncpForm = nodeCreateParamsFormM.bindFromRequest()
    adnNodeFormM.bindFromRequest().fold(
      {formWithErrors =>
        val renderFut = createAdnNodeRender(formWithErrors, ncpForm)
        debug("createAdnNodeSubmit(): Failed to bind form: \n" + formatFormErrors(formWithErrors))
        renderFut map {
          NotAcceptable(_)
        }
      },
      {mnode0 =>
        val mnode1 = mnode0.copy(
          edges = mnode0.edges.copy(
            out = {
              val ownEdge = MEdge(MPredicates.OwnedBy, request.user.personIdOpt.get)
              MNodeEdges.edgesToMap(ownEdge)
            }
          )
        )
        for (adnId <- mnode1.save) yield {
          // Инициализировать новосозданный узел.
          maybeInitializeNode(ncpForm, adnId)
          // Отредиректить админа в созданный узел.
          Redirect(routes.SysMarket.showAdnNode(adnId))
            .flashing(FLASH.SUCCESS -> s"Создан узел сети: $adnId")
        }
      }
    )
  }

  /** При создании узла есть дополнительные отключаемые возможности по инициализации.
    * Тут фунцкия, отрабатывающая это дело. */
  private def maybeInitializeNode(ncpForm: Form[NodeCreateParams], adnId: String)(implicit messages: Messages): Future[_] = {
    lazy val logPrefix = s"maybeInitializeNode($adnId):"
    ncpForm.value match {
      case Some(ncp) =>
        // Все флаги отрабатываются аналогично, поэтому общий код вынесен за скобки.
        def f(flag: Boolean, errMsg: => String)(action: => Future[_]): Future[_] = {
          if (flag) {
            val fut: Future[_] = action
            fut onFailure { case ex: Throwable =>
              warn(errMsg, ex)
            }
            fut
          } else {
            Future successful None
          }
        }
        val billFut = f(ncp.billInit, s"$logPrefix Failed to initialize billing") {
          nodesUtil.createUserNodeBilling(adnId)
        }
        val etgsFut = f(ncp.extTgsInit, s"$logPrefix Failed to create default targets") {
          nodesUtil.createExtDfltTargets(adnId)(messages)
        }
        val madsFut = f(ncp.withDfltMads, s"$logPrefix Failed to install default mads") {
          nodesUtil.installDfltMads(adnId)(messages)
        }
        billFut flatMap { _ =>
          etgsFut flatMap { _ =>
            madsFut
          }
        }
      case None =>
        warn(s"$logPrefix Failed to bind ${NodeCreateParams.getClass.getSimpleName} form:\n ${formatFormErrors(ncpForm)}")
        Future successful None
    }
  }


  /** Страница с формой редактирования узла ADN. */
  def editAdnNode(adnId: String) = IsSuNodeGet(adnId).async { implicit request =>
    import request.mnode
    val formFilled = adnNodeFormM.fill(mnode)
    editAdnNodeBody(adnId, formFilled)
      .map { Ok(_) }
  }

  private def editAdnNodeBody(adnId: String, form: Form[MNode])
                             (implicit request: INodeReq[AnyContent]): Future[Html] = {
    val res = editAdnNodeFormTpl(request.mnode, form)
    Future successful res
  }

  /** Самбит формы редактирования узла. */
  def editAdnNodeSubmit(adnId: String) = IsSuNodePost(adnId).async { implicit request =>
    import request.mnode
    val formBinded = adnNodeFormM.bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        debug(s"editAdnNodeSubmit($adnId): Failed to bind form: ${formatFormErrors(formWithErrors)}")
        editAdnNodeBody(adnId, formWithErrors)
          .map(NotAcceptable(_))
      },
      {adnNode2 =>
        for {
          _ <- MNode.tryUpdate(mnode) { updateAdnNode(_, adnNode2) }
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
    val eActsFut = EmailActivation.findByKey(adnId)
    eActsFut map { eActs =>
      Ok(nodeOwnerInvitesTpl(request.mnode, nodeOwnerInviteFormM, eActs))
    }
  }

  /** Сабмит формы создания инвайта на управление ТЦ. */
  def nodeOwnerInviteFormSubmit(adnId: String) = IsSuNodePost(adnId).async { implicit request =>
    import request.mnode
    nodeOwnerInviteFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"martInviteFormSubmit($adnId): Failed to bind form: ${formWithErrors.errors}")
        EmailActivation.findByKey(adnId) map { eActs =>
          NotAcceptable(nodeOwnerInvitesTpl(mnode, formWithErrors, eActs))
        }
      },
      {email1 =>
        val eAct = EmailActivation(email=email1, key = adnId)
        eAct.save.map { eActId =>
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
  def showAdnNodeAds(a: AdSearch) = IsSuperuserGet.async { implicit request =>

    // Ищем все рекламные карточки, подходящие под запрос.
    // TODO Нужна устойчивая сортировка.
    val madsFut = MNode.dynSearch(a)
    val brArgssFut = madsFut flatMap { mads =>
      Future.traverse(mads) { mad =>
        lkAdUtil.tiledAdBrArgs(mad)
      }
    }

    /** Сбор id'шников в критериях поиска первого попавшегося узла. */
    def _nodesF(p: MPredicate): Seq[String] = {
      a.outEdges
        .iterator
        .filter { _.predicates.contains(p) }
        .flatMap( _.nodeIds )
        .toStream
    }
    val producerIds = _nodesF( MPredicates.OwnedBy )
    val rcvrIds = _nodesF( MPredicates.Receiver )

    // Узнаём текущий узел на основе запроса. TODO Кривовато это как-то, может стоит через аргумент передавать?
    val adnNodeIdOpt = {
      producerIds
        .headOption
        .orElse {
          rcvrIds.headOption
        }
    }

    // Узнаём статистику по просмотрам/кликам. TODO Оно актуально тут ещё?
    val adFreqsFut: Future[AdFreqs_t] = adnNodeIdOpt
      .fold [Future[MAdStat.AdFreqs_t]] {
        Future successful Map.empty
      } {
        MAdStat.findAdByActionFreqs
      }
    val adnNodeOptFut = FutureUtil.optFut2futOpt(adnNodeIdOpt)( mNodeCache.getById )

    // Собираем карту размещений рекламных карточек.
    val ad2advMapFut: Future[Map[String, Seq[MAdvI]]] = {
      for {
        mads <- madsFut
        advs <- {
          lazy val adIds = mads.flatMap(_.id)
          val _advs: Future[Seq[MAdvI]] = if (rcvrIds.nonEmpty) {
            // Ищем все размещения имеющихся карточек у запрошенных ресиверов.
            Future {
              db.withConnection { implicit c =>
                MAdvOk.findByAdIdsAndRcvrs(adIds, rcvrIds = rcvrIds) ++
                  MAdvReq.findByAdIdsAndRcvrs(adIds, rcvrIds = rcvrIds)
              }
            }(AsyncUtil.jdbcExecutionContext)

          } else if (producerIds.nonEmpty) {
            // Ищем размещения карточек для продьюсера.
            Future {
              db.withConnection { implicit c =>
                MAdvOk.findByAdIdsAndProducersOnline(adIds, prodIds = producerIds, isOnline = true) ++
                  MAdvReq.findByAdIdsAndProducers(adIds, prodIds = producerIds)
              }
            }(AsyncUtil.jdbcExecutionContext)

          } else {
            Future successful Nil
          }
          _advs
        }
      } yield {
        advs.groupBy(_.adId)
      }
    }

    // Собираем ресиверов рекламных карточек.
    val rcvrsFut: Future[Map[String, Seq[MNode]]] = if (rcvrIds.nonEmpty) {
      // Используем только переданные ресиверы.
      Future
        .traverse(rcvrIds) { mNodeCache.getById }
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
          val allRcvrIds = ad2advsMap.foldLeft(List.empty[String]) {
            case (acc0, (_, advs)) =>
              advs.foldLeft(acc0) {
                (acc1, adv) => adv.rcvrAdnId :: acc1
              }
          }
          mNodeCache.multiGet(allRcvrIds.toSet)
        }
      } yield {
        // Список ресиверов конвертим в карту ресиверов.
        val rcvrsMap = allRcvrs.map { rcvr => rcvr.id.get -> rcvr }.toMap
        // Заменяем в исходной карте ad2advs списки adv на списки ресиверов.
        ad2advsMap.mapValues { advs =>
          advs.flatMap { adv =>
            rcvrsMap.get( adv.rcvrAdnId )
          }
        }
      }
    }

    // Планируем рендер страницы-результата, когда все данные будут собраны.
    for {
      adFreqs       <- adFreqsFut
      brArgss       <- brArgssFut
      adnNodeOpt    <- adnNodeOptFut
      rcvrs         <- rcvrsFut
      ad2advMap     <- ad2advMapFut
    } yield {
      val rargs = MShowNodeAdsTplArgs(brArgss, adnNodeOpt, adFreqs, rcvrs, a, ad2advMap)
      Ok( showAdnNodeAdsTpl(rargs) )
    }
  }


  /** Убрать указанную рекламную карточку из выдачи указанного ресивера. */
  def removeAdRcvr(adId: String, rcvrIdOpt: Option[String], r: Option[String]) = {
    IsSuperuserMadPost(adId).async { implicit request =>
      // Запускаем спиливание ресивера для указанной рекламной карточки.
      val isOkFut = advUtil.removeAdRcvr(adId, rcvrIdOpt = rcvrIdOpt)

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
          val adSearch = AdSearch.byRcvrId(rcvrId)
          routes.SysMarket.showAdnNodeAds(adSearch)
        }
        Future.successful( call )
      }

      // Дождаться завершения всех операций.
      for {
        rdr  <- rdrToFut
        isOk <- isOkFut
      } yield {
        // Вернуть редирект с результатом работы.
        val flasher = if (isOk) {
          FLASH.SUCCESS -> "Карточка убрана из выдачи."
        } else {
          FLASH.ERROR   -> "Карточка не найдена."
        }
        rdr.flashing(flasher)
      }
    }
  }


  /** Отобразить email-уведомление об отключении указанной рекламы. */
  def showShopEmailAdDisableMsg(adId: String) = IsSuperuserMad(adId).async { implicit request =>
    import request.mad

    // Получить ТЦ.
    val mmartFut = n2NodesUtil
      .receiverIds(mad)
      .toStream
      .headOption
      .fold [Future[Option[MNode]]] {
        // Тут хрень какая-то. Наугад выбирается случайный узел.
        MNode.dynSearchOne {
          new MNodeSearchDfltImpl {
            override def nodeTypes = Seq( MNodeTypes.AdnNode )
            override def withAdnRights = Seq( AdnRights.RECEIVER )
            override def limit = 1
          }
        }
      } {
        MNode.getById(_)
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
   * @param adId id рекламной карточки.
   */
  def showAd(adId: String) = IsSuperuserMad(adId).async { implicit request =>
    import request.mad

    // Определить узла-продьюсера
    val producerIdOpt = n2NodesUtil.madProducerId( mad )
    val producerOptFut = mNodeCache.maybeGetByIdCached( producerIdOpt )

    // Собрать инфу по картинкам.
    val imgs = {
      mad.edges.out
        .valuesIterator
        .filter { e =>
          e.predicate.toTypeValid( MNodeTypes.Media.Image )
        }
        .map { e =>
          MImgEdge(e, mImgs3(e))
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
  def analyzeAdRcvrs(adId: String) = IsSuperuserMadGet(adId).async { implicit request =>
    import request.mad
    val producerId = n2NodesUtil.madProducerId(mad).get
    val producerOptFut = mNodeCache.getById(producerId)
    val newRcvrsMapFut = producerOptFut
      .flatMap { advUtil.calculateReceiversFor(mad, _) }

    val rcvrsMap = n2NodesUtil.receiversMap(mad)

    // Достаём из кеша узлы.
    val nodesMapFut: Future[Map[String, MNode]] = {
      def _nodeIds(rcvrs: Receivers_t) = rcvrs.keysIterator.map(_._2).toSet
      val adnIds1 = _nodeIds(rcvrsMap)
      for {
        adns1       <- mNodeCache.multiGet(adnIds1)
        newRcvrsMap <- newRcvrsMapFut
        newAdns     <- {
          val newRcvrIds = _nodeIds(newRcvrsMap)
          mNodeCache.multiGet(newRcvrIds -- adnIds1)
        }
      } yield {
        (adns1.iterator ++ newAdns.iterator)
          .map { adnNode => adnNode.id.get -> adnNode }
          .toMap
      }
    }

    // Узнать, совпадает ли рассчетная карта ресиверов с текущей.
    val rcvrsMapOkFut = for (newRcvrsMap <- newRcvrsMapFut) yield {
      newRcvrsMap == rcvrsMap
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
  def resetReceivers(adId: String, r: Option[String]) = IsSuperuserMadPost(adId).async { implicit request =>
    for {
      // Вычислить ресиверов согласно биллингу и прочему.
      newRcvrs <- advUtil.calculateReceiversFor(request.mad)

      // Запустить обновление ресиверов в карте.
      _adId    <- {
        MNode.tryUpdate(request.mad) { mad =>
          mad.copy(
            edges = mad.edges.copy(
              out = {
                val oldEdgesIter = mad.edges
                  .withoutPredicateIter( MPredicates.Receiver )
                val newRcvrEdges = newRcvrs.valuesIterator
                MNodeEdges.edgesToMap1( oldEdgesIter ++ newRcvrEdges )
              }
            )
          )
        }
      }

    } yield {
      // Когда всё будет сделано, отредиректить юзера назад на страницу ресиверов.
      RdrBackOr(r) {
        routes.SysMarket.analyzeAdRcvrs(adId)
      }
        .flashing(FLASH.SUCCESS -> s"Произведён сброс ресиверов узла. Теперь ${newRcvrs.size} ресиверов.")
    }
  }


  /** Очистить полностью таблицу ресиверов. Бывает нужно для временного сокрытия карточки везде.
    * Это действие можно откатить через resetReceivers. */
  def cleanReceivers(adId: String, r: Option[String]) = IsSuperuserMadPost(adId).async { implicit request =>
    for {
      _adId <- {
        MNode.tryUpdate(request.mad) { mad =>
          mad.copy(
            edges = mad.edges.copy(
              out = {
                val iter = mad.edges
                  .withoutPredicateIter( MPredicates.Receiver )
                MNodeEdges.edgesToMap1(iter)
              }
            )
          )
        }
      }
    } yield {
      RdrBackOr(r) {
        routes.SysMarket.analyzeAdRcvrs(adId)
      }
        .flashing(FLASH.SUCCESS -> "Из узла вычищены все ребра ресиверов. Биллинг не затрагивался.")
    }
  }

}

