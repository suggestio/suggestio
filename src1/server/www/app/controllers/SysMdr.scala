package controllers

import akka.stream.scaladsl.{Keep, Sink, Source}
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.common.empty.OptionUtil
import javax.inject.{Inject, Singleton}
import io.suggest.ctx.CtxData
import io.suggest.err.ErrorConstants
import io.suggest.es.model.MEsUuId
import io.suggest.init.routed.MJsInitTargets
import io.suggest.maps.nodes.MAdvGeoMapNodeProps
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.node.meta.colors.MColors
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.req.ReqUtil
import io.suggest.sys.mdr._
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import play.api.libs.json.Json
import util.acl._
import views.html.sys1.mdr._
import views.html.lk.mdr._
import japgolly.univeq._
import models.mctx.Context
import models.req.{MNodesChainReq, MReq}
import play.api.mvc.{ActionBuilder, AnyContent}
import util.ad.JdAdUtil
import util.adn.NodesUtil
import util.mdr.MdrUtil

import scala.concurrent.Future
import scala.util.Success

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.14 10:45
 * Description: Sys Moderation - контроллер, заправляющий s.io-модерацией рекламных карточек.
 */
@Singleton
class SysMdr @Inject() (
                         jdAdUtil                 : JdAdUtil,
                         mNodes                   : MNodes,
                         reqUtil                  : ReqUtil,
                         isSuNode                 : IsSuNode,
                         isSu                     : IsSu,
                         mdrUtil                  : MdrUtil,
                         isNodeAdmin              : IsNodeAdmin,
                         canMdrResolute           : CanMdrResolute,
                         nodesUtil                : NodesUtil,
                         isAuth                   : IsAuth,
                         override val mCommonDi   : ICommonDi,
                       )
  extends SioControllerImpl
  with MacroLogsImpl
{

  import mCommonDi._
  import slick.profile.api._


  /** react-форма для осуществления модерации в /sys/.
    *
    * @return Страница под react-форму.
    */
  def sysPage = csrf.AddToken {
    isSu().async { implicit request =>
      implicit val ctxData = CtxData(
        jsInitTargets = MJsInitTargets.SysMdrForm :: Nil
      )
      val formState = Json.toJson( MMdrConf.Variants.sys ).toString()
      Ok( SysMdrFormTpl(formState) )
    }
  }


  /** Страница модерации карточек на узле.
    *
    * @param rcvrKey Ключ узла.
    * @return 200 OK с html-страницей личного кабинета модерации.
    */
  def lkMdr(rcvrKey: RcvrKey) = csrf.AddToken {
    isNodeAdmin(rcvrKey, U.Lk).async { implicit request =>
      // Готовим контекст...
      val ctxFut = request.user.lkCtxDataFut.map { lkCtxData =>
        implicit val ctxData2 = lkCtxData.withJsInitTargets(
          MJsInitTargets.SysMdrForm :: lkCtxData.jsInitTargets
        )
        implicitly[Context]
      }

      val formState = MMdrConf.Variants.lk( rcvrKey, isSu = request.user.isSuper )
      val formStateJson = Json.toJson( formState ).toString()

      // Рендер страницы и ответа клиенту.
      for {
        ctx <- ctxFut
      } yield {
        val html = LkMdrTpl(request.mnode, formStateJson)(ctx)
        Ok(html)
      }
    }
  }


  /** Получение данных следующей карточки.
    *
    * @param args Аргументы модерации.
    * @return 200 OK + JSON-ответ MMdrNextResp с данными модерируемого узла/карточки.
    */
  def nextMdrInfo(args: MdrSearchArgs) = csrf.Check {
    lazy val logPrefix = s"nextMdrInfo(${args.conf.rcvrIdOpt.getOrElse("")})#${System.currentTimeMillis()}:"

    // Сборка ActionBuilder'а, который проверит и подготовит данные для запуска экшена:
    // Если задан producerId, то надо организовать проверку на уровне юзера и личного кабинета.
    val ab = args.conf.onNodeKey.fold [ActionBuilder[MNodesChainReq, AnyContent]] {
      // Не задан id ресивера - значит только супер-юзер допустим.
      if (args.conf.isSu) {
        // Работа от имени супер-юзера вне какого-либо узла.
        isSu().andThen {
          new reqUtil.ActionTransformerImpl[MReq, MNodesChainReq] {
            override protected def transform[A](request: MReq[A]): Future[MNodesChainReq[A]] = {
              val nodeOptReq = MNodesChainReq(Nil, request, request.user)
              Future.successful( nodeOptReq )
            }
          }
        }
      } else {
        isAuth().andThen {
          new reqUtil.ActionTransformerImpl[MReq, MNodesChainReq] {
            override protected def transform[A](request: MReq[A]): Future[MNodesChainReq[A]] = {
              LOGGER.trace(s"$logPrefix PersonId#${request.user.personIdOpt.orNull} as parent-node")
              for {
                personNode <- request.user.personNodeFut
              } yield {
                MNodesChainReq(personNode :: Nil, request, request.user)
              }
            }
          }
        }
      }
    } { rcvrKey =>
      // Модерация в контексте указанного узла. Может быть и супер-юзер, и обычный владелец личного кабинета.
      LOGGER.trace(s"$logPrefix Mdr on node#${RcvrKey.rcvrKey2urlPath(rcvrKey)}")
      isNodeAdmin( rcvrKey )
    }

    // Сборка тела экшена: функция поиска и возврата данных узла для модерации:
    ab.async { implicit request =>
      LOGGER.trace(s"$logPrefix args=$args moderator#${request.user.personIdOpt.orNull}")

      // Если задан ресивер, то надо найти все дочерние под-узлы, включая текущий.
      val rcvrIdsFut = request.mnodeOpt
        .fold [Future[Set[String]]] {
          // Нет ресивера - значит sio-модератор/суперюзер листает всё в системе, что отправлено на модерацию.
          // В норме этот assert не нужен, т.к. проверки должны быть выполнены в ACL. Просто самоконтроль по мере развития и усложнения кода экшена.
          ErrorConstants.assertArg( request.user.isSuper )
          Future.successful( Set.empty )
        } { rcvrNode =>
          // Нельзя выполнять экшен на эфемерных узлах, т.к. отсутствие узла и его id
          val rcvrId = rcvrNode.id.get
          val rcvrIdSet = Set(rcvrId)

          // Сколько уровней children искать? Если пляшем от текущего юзера, то можно искать на один уровень глубже.
          val maxLevelsDeep = mdrUtil.maxLevelsDeepFor( request.user.personIdOpt contains rcvrId )

          for {
            childIds <- nodesUtil.collectChildIds( rcvrIdSet, maxLevelsDeep )
          } yield {
            LOGGER.trace(s"$logPrefix Collected ${childIds.size} child ids of parent#$rcvrId: [${childIds.mkString(", ")}]")
            childIds ++ rcvrIdSet
          }
        }

      // Заготовка sql-запроса, который занимается поиском немодерированных узлов в биллинге:
      val paidNodesSqlFut = for (rcvrIds <- rcvrIdsFut) yield {
        mdrUtil.findPaidNodeIds4MdrQ(
          hideNodeIdOpt = args.hideAdIdOpt,
          rcvrIds       = rcvrIds
        )
      }

      var errNodeIdsAcc = List.empty[String]

      /** Цикл поиска id узла, который требуется промодерировать.
        * Появился для отработки ситуации, когда база нарушена, и узел из nodeid уже удалён. */
      def _findBillNode4MdrOrNseeFut(args0: MdrSearchArgs = args): Future[MNode] = {
        if (args0.offset > 50)
          throw new IllegalArgumentException(s"$logPrefix Too may retries, too many invalid nodes.")

        // Поискать в биллинге узел, который надо модерировать:
        val mnodeFut = for {
          // По идее, paidNodesSql надо дожидаться вне этой функции-цикла, но тут не особо важно:
          // в норме эта функция вызывает максимум 1 раз, а при ошибках - оверхед низкий.
          paidNodesSql <- paidNodesSqlFut

          // Ищем следующую карточку через биллинг и очередь на модерацию.
          nodeIds <- slick.db.run {
            // TODO Добавить сортировку с учётом qs args0.gen, чтобы разные модераторы получали бы разные элементы для модерации.
            mdrUtil.getFirstIn( args0, paidNodesSql, limit = 1 )
          }

          // Возможна NSEE, это нормально. Обходим проблемы совместимости NSEE с Vector через headOption.get (вместо head):
          nodeId = nodeIds.headOption.get
          mnodeOpt <- mNodesCache.getById( nodeId )

        } yield {
          // Возможна ситуация, когда узел уже удалён, но в биллинге - ещё не модерирован. Модер должен принять решение об удалении.
          mnodeOpt.fold[MNode] {
            // item + node_id есть, а узел отсутствует. Может быть кластер развалился, а может это ошибка в базе. Пока просто логгируем.
            LOGGER.error(s"$logPrefix node $nodeId missing, but present in items\n req=${request.uri}")
            throw new IllegalStateException( nodeId )
          } { mnode =>
            LOGGER.trace(s"$logPrefix mdr node => ${mnodeOpt.flatMap(_.id)} (${nodeIds.length})")
            mnode
          }
        }

        mnodeFut.recoverWith { case ex: IllegalStateException =>
          val offset2 = args0.offset + 1
          LOGGER.trace(s"$logPrefix Will retry one more node [$offset2]")
          val nodeId = ex.getMessage
          val args2 = args0.copy(
            offsetOpt = Some( offset2 )
          )
          errNodeIdsAcc ::= nodeId
          _findBillNode4MdrOrNseeFut( args2 )
        }
      }

      // Поискать в биллинге узел, который надо модерировать:
      val billNodeWithErrorIdsOrNseeFut = _findBillNode4MdrOrNseeFut()
      val billedNodeOrExFut = billNodeWithErrorIdsOrNseeFut

      val errorNodeIdsFut = billNodeWithErrorIdsOrNseeFut
        .transform { case _ => Success(errNodeIdsAcc.toSet) }

      // Инстанс для поиска бесплатных модераций.
      lazy val freeMdrsSearchFut = for {
        rcvrIds <- rcvrIdsFut
      } yield {
        // TODO Добавить gen sort, где generation = args.gen, взятый например из personId.hashCode
        mdrUtil.freeMdrNodeSearchArgs(args, rcvrIds.toSeq, 1)
      }

      // Free-mdr пост-модерация доступна только для супер-юзеров и вторична.
      val isWithFreeMdr = request.user.isSuper

      // Точно узнать трудно, но можно примерно посчитать кол-во узлов в paid и free, выбрать наибольшее.
      val freeMdrsCountFut =
        if (isWithFreeMdr) freeMdrsSearchFut.flatMap( mNodes.dynCount )
        else Future.successful(0L)

      // Если не будет найдено биллинга для модерации, то надо поискать бесплатные немодерированные размещения.
      var billedOrFreeNodeOrExFut = billedNodeOrExFut
      if (isWithFreeMdr) {
        // поддержка пост-модерации free-узлов для суперюзеров, если bill-размещения закончились.
        billedOrFreeNodeOrExFut = billedOrFreeNodeOrExFut.recoverWith { case _: NoSuchElementException =>
          LOGGER.trace(s"$logPrefix No more paid advs, looking for free advs...\n $args")
          for {
            freeMdrsSearch <- freeMdrsSearchFut
            // Если нет paid-модерируемых карточек, то поискать бесплатные размещения.
            res <- mNodes.dynSearchOne( freeMdrsSearch )
          } yield {
            res.get
          }
        }
      }

      // Надо оценить итоговую длину очереди на модерацию.
      val mdrQueueReportFut = for {
        paidNodesSql      <- paidNodesSqlFut
        paidMdrNodesCount <- slick.db.run {
          paidNodesSql.size.result
        }
        freeMdrsCount     <- freeMdrsCountFut
      } yield {
        val minQueueLen = Math.max( freeMdrsCount.toInt, paidMdrNodesCount )
        val maybeHaveMore = freeMdrsCount > 0 && paidMdrNodesCount > 0
        LOGGER.trace(s"$logPrefix Mdr queue lenghts: free?${isWithFreeMdr}=$freeMdrsCount paid=$paidMdrNodesCount => report=$minQueueLen${if(maybeHaveMore) "+" else ""}")
        MMdrQueueReport(minQueueLen, maybeHaveMore)
      }

      implicit val ctx = implicitly[Context]

      // Нужно запустить сборку списков item'ов на модерацию.
      val mitemsFut = for {
        mdrNode <- billedOrFreeNodeOrExFut
        nodeId = mdrNode.id.get
        items <- slick.db.run {
          mdrUtil.itemsQueryAwaiting( nodeId )
            // Ограничиваем кол-во запрашиваемых item'ов. Нет никакого смысла вываливать слишком много данных на экран.
            .take(50)
            // Тяжелая сортировка тут скорее всего не важна, поэтому опускаем её.
            .result
        }
      } yield {
        LOGGER.trace(s"$logPrefix Found ${items.length} awaitingMdr-items for node#$nodeId")
        items
      }

      // Запустить jd-рендер целиком, если это рекламная карточка:
      val jdAdDataSomeOrExFut = for {
        mad <- billedOrFreeNodeOrExFut
        if {
          LOGGER.trace( s"$logPrefix is jd ad? ntype=${mad.common.ntype} doc=${mad.extras.doc}" )
          (mad.common.ntype ==* MNodeTypes.Ad) && mad.extras.doc.nonEmpty
        }

        // И организуется jd-рендер:
        tpl = jdAdUtil.getNodeTpl( mad )
        edges2 = jdAdUtil.filterEdgesForTpl(tpl, mad.edges)
        jdAdData <- jdAdUtil
          .mkJdAdDataFor
          .show(
            nodeId        = mad.id,
            nodeEdges     = edges2,
            tpl           = tpl,
            szMult        = SysMdrConst.SZ_MULT.toFloat,
            allowWide     = true,
            forceAbsUrls  = false
          )(ctx)
          .execute()

      } yield {
        Some( jdAdData )
      }

      // Сбор данных по бесплатным self-ресиверам
      val selfRcvrsNeedMdrFut = for {
        mad <- billedNodeOrExFut
      } yield {
        val selfEdges = mad.edges
          .withPredicateIter( MPredicates.Receiver.Self )
          .toStream
        val isMdrNotNeeded = selfEdges.isEmpty || {
          // Уже есть mdr-true-эдж?
          mad.edges
            .withPredicateIter( MPredicates.ModeratedBy )
            .exists(_.info.flag contains true)
        }
        if (isMdrNotNeeded) {
          Set.empty[String]
        } else {
          selfEdges
            .iterator
            .flatMap(_.nodeIds)
            .toSet
        }
      }

      // Сборка данных по узлам, требующихся для рендера
      val nodesRenderedFut = for {
        mitems <- mitemsFut
        // Собрать id узлов, связанных с размещениями.
        itemRcvrsSet = mitems
          .iterator
          .flatMap(_.rcvrIdOpt)
          .toSet

        selfRcvrsNeedMdr <- selfRcvrsNeedMdrFut

        // Надо получить на руки модерируемый узел, чтобы понять, надо ли его добавлять в список ренденных узлов.
        mdrNode <- billedOrFreeNodeOrExFut
        isAddMdrNodeToNodes = mdrNode.common.ntype ==* MNodeTypes.AdnNode

        // Собрать все id всех затрагиваемых узлов.
        needNodeIds = {
          var nodeIds = selfRcvrsNeedMdr ++ itemRcvrsSet
          // Выкинуть id текущего узла из списка получаемых из БД:
          if (isAddMdrNodeToNodes && nodeIds.contains(mdrNode.id.get))
            nodeIds = nodeIds - mdrNode.id.get
          nodeIds
        }

        nodesRendered <- {
          var src0 = mNodesCache
            .multiGetSrc( needNodeIds )
          // Если текущий узел тоже требуется рендерить, то добавить его готовый инстанс в начало:
          if (isAddMdrNodeToNodes)
            src0 = Source.single( mdrNode ) ++ src0

          // Отрендерить все узлы:
          src0
            .map { mnode =>
              // Пока интересует только название целевого узла.
              MAdvGeoMapNodeProps(
                nodeId  = mnode.id.get,
                ntype   = mnode.common.ntype,
                colors  = MColors.empty, //mnode.meta.colors,
                hint    = mnode.guessDisplayName
              )
            }
            .toMat( Sink.seq[MAdvGeoMapNodeProps] )( Keep.right )
            .run()
        }

      } yield {
        LOGGER.trace(s"$logPrefix Rendered ${nodesRendered.length} nodes into props.")
        nodesRendered
      }

      // Сборка нормального положительного ответа на запрос:
      val nodeInfoOptFut = (
        for {
          mdrNode      <- billedOrFreeNodeOrExFut
          jdAdDataOpt  <- jdAdDataSomeOrExFut
            .recover { case _: NoSuchElementException => None }

          mitems            <- mitemsFut
          selfRcvrsNeedMdr  <- selfRcvrsNeedMdrFut
          nodes             <- nodesRenderedFut
        } yield {
          val respNodeId = mdrNode.id.get
          LOGGER.trace(s"$logPrefix Done, returning mdr data for node#$respNodeId...")
          val nodeMdr = MNodeMdrInfo(
            nodeId  = respNodeId,
            ad      = jdAdDataOpt,
            items   = mitems,
            nodes   = nodes,
            directSelfNodeIds = selfRcvrsNeedMdr,
          )
          Some(nodeMdr)
        }
      )
        // Не найдено узла? Это нормально, бывает.
        .recover { case _: NoSuchElementException =>
          LOGGER.trace(s"$logPrefix No more nodes for moderation.")
          None
        }

      // Сборка итогового JSON-ответа.
      for {
        errorNodeIds   <- errorNodeIdsFut
        mdrQueueReport <- mdrQueueReportFut
        nodeInfoOpt    <- nodeInfoOptFut
      } yield {
        val resp = MMdrNextResp(
          nodeOpt       = nodeInfoOpt,
          errorNodeIds  = errorNodeIds,
          mdrQueue      = mdrQueueReport
        )
        Ok( Json.toJson(resp) )
      }
    }
  }


  /** Пришла команда от модератора об изменении состояния элементов модерации.
    *
    * @param mdrRes Контейнер данных по одному действию модерации.
    * @return 204 No Content, когда всё ок.
    */
  def doMdr(mdrRes: MMdrResolution) = csrf.Check {
    canMdrResolute(mdrRes).async { implicit request =>
      // Т.к. решение модерации является *маской* группы item'ов - надо собрать ресиверы.
      // Без конкретных ресиверов может быть только su-модерация.
      val ignoreRcvrs = request.user.isSuper || request.mitemOpt.flatMap(_.rcvrIdOpt).nonEmpty
      val rcvrIdsOptFut = OptionUtil.maybeFut( !ignoreRcvrs ) {
        // Нужно собрать множество id ресиверов текущего юзера или текущего узла.
        val (parentRcvrId, isPerson) = mdrRes.conf.rcvrIdOpt
          .fold(request.user.personIdOpt.get -> true)(_ -> false)
        val rcvrsLevelsDeep = mdrUtil.maxLevelsDeepFor(isPerson)
        for {
          rcvrIds <- nodesUtil.collectChildIds( Set(parentRcvrId), rcvrsLevelsDeep )
        } yield {
          Some( rcvrIds )
        }
      }

      // Надо организовать пакетное обновления в БД биллинга, в зависимости от значений полей резолюшена.
      for {
        rcvrIdsOpt <- rcvrIdsOptFut
        rcvrIds = rcvrIdsOpt getOrElse Set.empty
        _ <- mdrUtil.processMdrResolution( mdrRes, request.mnode, request.user, rcvrIds )
      } yield {
        // Вернуть ответ -- обычно ничего возвращать не требуется.
        NoContent
      }
    }
  }


  /** Авто-ремонт узла, который запускает автоматическое исправление проблем с узлом.
    *
    * @param nodeId id узла с которым есть проблемы модерации.
    * @return 204 No Content, если всё ок.
    */
  def fixNode(nodeId: MEsUuId) = csrf.Check {
    // Сразу проверяем, что узел отсутсвует. Тогда его можно чинить.
    isSuNode.nodeMissing(nodeId).async { implicit request =>
      val refuseReasonOpt: Option[String] = Some( "fixNode" )
      for {
        _ <- mdrUtil.fixNode(nodeId.id, refuseReasonOpt )
      } yield {
        NoContent
      }
    }
  }

}

