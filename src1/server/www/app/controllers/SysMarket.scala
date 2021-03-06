package controllers

import javax.inject.Inject
import io.suggest.adn.MAdnRights
import io.suggest.es.model.{EsModel, MEsNestedSearch, MEsUuId}
import io.suggest.i18n.MsgCodes
import io.suggest.n2.edge.{MEdge, MNodeEdges, MPredicates}
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.meta.MMeta
import io.suggest.n2.node.{MNode, MNodeType, MNodeTypes, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.sec.util.Csrf
import io.suggest.session.MSessionKeys
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.Context
import models.msys._
import models.req.{INodeReq, IReq, IReqHdr, MNodeReq}
import models.usr.MEmailRecoverQs
import org.elasticsearch.search.sort.SortOrder
import play.api.data._
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Result}
import util.acl._
import util.adn.NodesUtil
import util.mail.IMailerWrapper
import util.n2u.N2NodesUtil
import util.sys.SysMarketUtil
import views.html.lk.adn.invite.emailNodeOwnerInviteTpl
import views.html.lk.shop.ad.emailAdDisabledByMartTpl
import views.html.sys1.market._
import views.html.sys1.market.adn._

import scala.concurrent.Future
import japgolly.univeq._
import models.madn.{AdnShownTypes, NodeDfltColors}
import play.api.http.HttpErrorHandler
import util.ident.IdentUtil
import util.tpl.HtmlCompressUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.02.14 17:21
 * Description: Тут управление компаниями, торговыми центрами и магазинами.
 */
final class SysMarket @Inject() (
                                  sioControllerApi                : SioControllerApi,
                                )
  extends MacroLogsImpl
{

  import sioControllerApi._

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val nodesUtil = injector.instanceOf[NodesUtil]
  private lazy val sysMarketUtil = injector.instanceOf[SysMarketUtil]
  private lazy val mailer = injector.instanceOf[IMailerWrapper]
  private lazy val n2NodesUtil = injector.instanceOf[N2NodesUtil]
  private lazy val isSuNode = injector.instanceOf[IsSuNode]
  private lazy val isSuMad = injector.instanceOf[IsSuMad]
  private lazy val isSu = injector.instanceOf[IsSu]
  private lazy val isSuOr404 = injector.instanceOf[IsSuOr404]
  private lazy val identUtil = injector.instanceOf[IdentUtil]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val htmlCompressUtil = injector.instanceOf[HtmlCompressUtil]
  private lazy val csrf = injector.instanceOf[Csrf]
  private lazy val errorHandler = injector.instanceOf[HttpErrorHandler]

  import esModel.api._


  /**
   * Корень /sys/ indexTpl.scala.html для системной панели.
   *
   * Изначально оно жило в ctl.Sys, который был замёржен в ctl.Application,
   * который тоже был упразднён 2015.dec.17.
   */
  def sysIndex() = csrf.AddToken {
    isSuOr404() { implicit request =>
      Ok( views.html.sys1.indexTpl() )
    }
  }


  /** Корень /sys/marker/. Тут ссылки на дальнейшие страницы в рамках market. */
  def index() = csrf.AddToken {
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
      val msearch = new MNodeSearch {
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
          .toList
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
        allNti :: ntypes0
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

      def _prepareEdgeInfos(eis: Iterator[MNodeEdgeInfo]): Seq[MNodeEdgeInfo] = {
        eis
          .toSeq
          .sortBy { ei =>
            // Собрать ключ для сортировки
            val nodeName = ei.mnodeEiths
              .iterator
              .map { _.fold [String] (identity, _.guessDisplayNameOrId.getOrElse("")) }
              .nextOption()
              .getOrElse("???")
            (ei.medge.predicate.value,
              ei.medge.order.getOrElse(Int.MaxValue),
              nodeName)
          }
      }

      val mnodesMapFut = mNodes.multiGetMapCache {
        mnode.edges
          .out
          .iterator
          .flatMap(_.nodeIds)
          .toSet
      }

      // Узнаём исходящие ребра.
      val outEdgesFut: Future[Seq[MNodeEdgeInfo]] = for {
        nmap <- mnodesMapFut
      } yield {
        val iter = for {
          (medge, index) <- mnode.edges
            .out
            .iterator
            .zipWithIndex
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
        val msearch = new MNodeSearch {
          override val outEdges: MEsNestedSearch[Criteria] = {
            val cr = Criteria(nodeIds = nodeId :: Nil)
            MEsNestedSearch.plain( cr )
          }
          override def limit = 200
        }
        for {
          mnodes <- mNodes.dynSearch( msearch )
        } yield {
          val iter = for {
            mnode <- mnodes.iterator
            medge <- mnode.edges.withNodeId( nodeId )
          } yield {
            MNodeEdgeInfo(
              medge       = medge,
              mnodeEiths  = Right(mnode) :: Nil,
              edgeId      = None
            )
          }

          _prepareEdgeInfos(iter)
        }
      }

      // Определить имена юзеров-владельцев.
      val personNamesFut = for (nmap <- mnodesMapFut) yield {
        (for {
          nodeId <- mnode.edges
            .withPredicateIterIds( MPredicates.OwnedBy )
            .toSet[String]
            .iterator
          mnode <- nmap.get( nodeId ).iterator
        } yield {
          nodeId -> mnode.guessDisplayNameOrIdOrQuestions
        })
          .toMap
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
    isSuNode(nodeId).async { implicit request =>
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
            LOGGER.warn(s"deleteAdnNodeSubmit($nodeId): $msg")
            errorHandler.onClientError(request, NOT_FOUND, msg)
        }
    }
  }


  /** Страница с формой создания нового узла. */
  def createAdnNode() = csrf.AddToken {
    isSu().async { implicit request =>
      // Генерим stub и втыкаем его в форму, чтобы меньше галочек ставить.
      // 2015.oct.21: Используем nodesUtil для сборки дефолтового инстанса.
      val dfltFormM = sysMarketUtil.adnNodeFormM.fill(
        nodesUtil.userNodeInstance(
          nameOpt     = None,
          personIdOpt = request.user.personIdOpt
        )
      )

      val ncpForm = sysMarketUtil.nodeCreateParamsFormM.fill( NodeCreateParams() )

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
      val ncpForm = sysMarketUtil.nodeCreateParamsFormM.bindFromRequest()
      val nodeForm = sysMarketUtil.adnNodeFormM.bindFromRequest()

      def __onError(formWithErrors: Form[_], formName: String): Future[Result] = {
        val renderFut = createAdnNodeRender(nodeForm, ncpForm, NotAcceptable)
        LOGGER.debug(s"$logPrefix Failed to bind $formName form: \n${formatFormErrors(formWithErrors)}")
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
                    MMeta.colors.replace( colors2 )(mnode0.meta)
                  } else {
                    mnode0.meta
                  }
                )

                mnode2 <- mNodes.saveReturning(mnode1)
                nodeId = mnode2.id.get

              } yield {
                // Инициализировать новосозданный узел согласно заданным параметрам.
                maybeInitializeNode(ncp, nodeId)

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
          LOGGER.warn(errMsg, ex)
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
      val formFilled = sysMarketUtil.adnNodeFormM.fill(mnode)
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
      val formBinded = sysMarketUtil.adnNodeFormM.bindFromRequest()
      formBinded.fold(
        {formWithErrors =>
          LOGGER.debug(s"editAdnNodeSubmit($adnId): Failed to bind form: ${formatFormErrors(formWithErrors)}")
          editAdnNodeBody(adnId, formWithErrors, NotAcceptable)
        },
        {adnNode2 =>
          for {
            _ <- mNodes.tryUpdate(mnode) { sysMarketUtil.updateAdnNode(_, adnNode2) }
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
      _nodeOwnerInviteFormSubmit( sysMarketUtil.nodeOwnerInviteFormM, Ok)
    }
  }

  private def _nodeOwnerInviteFormSubmit(form: Form[String], rs: Status)(implicit request: MNodeReq[_]): Future[Result] = {
    val html = nodeOwnerInvitesTpl(request.mnode, sysMarketUtil.nodeOwnerInviteFormM)
    rs(html)
  }

  /** Сабмит формы создания инвайта на управление ТЦ. */
  def nodeOwnerInviteFormSubmit(adnId: String) = csrf.Check {
    isSuNode(adnId).async { implicit request =>
      import request.mnode
      sysMarketUtil.nodeOwnerInviteFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug(s"martInviteFormSubmit($adnId): Failed to bind form: ${formWithErrors.errors}")
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


  /** Отобразить email-уведомление об отключении указанной рекламы. */
  def showShopEmailAdDisableMsg(adId: String) = isSuMad(adId).async { implicit request =>
    import request.mad

    // Получить ТЦ.
    val mmartFut = n2NodesUtil
      .receiverIds(mad)
      .nextOption()
      .fold [Future[Option[MNode]]] {
        // Тут хрень какая-то. Наугад выбирается случайный узел.
        mNodes.dynSearchOne {
          new MNodeSearch {
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


  /** Залогинится в указанный узел.
    *
    * @param nodeIdU id узла.
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
          .withSession(MSessionKeys.PersonId.value -> nodeId)
      }
    }
  }


  /** Убрать указанный узел из кэша, вернув редирект. */
  def unCacheNode( nodeIdU: MEsUuId, r: Option[String] ) = csrf.Check {
    val nodeId = nodeIdU.id
    isSuNode(nodeId).async { implicit request =>
      for {
        _ <- mNodes.deleteFromCache( nodeId )
      } yield {
        r.fold( Redirect(routes.SysMarket.showAdnNode(nodeId)) )(Redirect(_: String, SEE_OTHER))
          .flashing( FLASH.SUCCESS -> s"Кэш сброшен для узла: $nodeId" )
      }
    }
  }


  /** SuperUser wants to temporary drop superuser priveledges. */
  def setNoSu() = csrf.Check {
    isSu().async { implicit request =>
      for {
        call <- identUtil.redirectCallUserSomewhere( request.user.personIdOpt.get )
      } yield {
        Redirect( call )
          .addingToSession( MSessionKeys.NoSu.value -> true.toString )
      }
    }
  }

}

