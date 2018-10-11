package controllers

import akka.stream.scaladsl.{Keep, Sink, Source}
import javax.inject.{Inject, Singleton}
import io.suggest.ctx.CtxData
import io.suggest.init.routed.MJsInitTargets
import io.suggest.maps.nodes.MAdvGeoMapNodeProps
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.node.meta.colors.MColors
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.sys.mdr._
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import play.api.libs.json.Json
import util.acl._
import util.mdr.SysMdrUtil
import views.html.sys1.mdr._
import japgolly.univeq._
import models.mctx.Context
import util.ad.JdAdUtil

import scala.concurrent.Future

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
                         isSuNode                 : IsSuNode,
                         isSu                     : IsSu,
                         sysMdrUtil               : SysMdrUtil,
                         override val mCommonDi   : ICommonDi,
                       )
  extends SioControllerImpl
  with MacroLogsImpl
{

  import mCommonDi._
  import slick.profile.api._


  /** react-форма для осуществления модерации.
    *
    * @return Страница под react-форму.
    */
  def index = csrf.AddToken {
    isSu().async { implicit request =>
      implicit val ctxData = CtxData(
        jsInitTargets = MJsInitTargets.SysMdrForm :: Nil
      )
      Ok( SysMdrFormTpl() )
    }
  }


  /** Получение данных следующей карточки.
    *
    * @param args Аргументы модерации.
    * @return 200 OK +  JSON-ответ MNodeMdrInfo с данными модерируемого узла/карточки.
    *         204 No content - если больше нечего модерировать.
    */
  def nextMdrInfo(args: MdrSearchArgs) = csrf.Check {
    isSu().async { implicit request =>
      lazy val logPrefix = s"nextMdrInfo()#${System.currentTimeMillis()}:"
      LOGGER.trace(s"$logPrefix args=$args moderator#${request.user.personIdOpt.orNull}")

      /** Цикл поиска id узла, который требуется промодерировать.
        * Появился для отработки ситуации, когда база нарушена, и узел из nodeid уже удалён. */
      def _findBillNode4MdrOrNseeFut(args0: MdrSearchArgs = args,
                                     errNodeIdAcc: List[String] = Nil): Future[(MNode, List[String])] = {
        if (args0.offset > 50)
          throw new IllegalArgumentException(s"$logPrefix Too may retries, too many invalid nodes.")

        // Поискать в биллинге узел, который надо модерировать:
        val mnodeFut = for {
          // Ищем следующую карточку через биллинг и очередь на модерацию.
          nodeIds <- slick.db.run {
            // TODO Добавить сортировку с учётом qs args0.gen, чтобы разные модераторы получали бы разные элементы для модерации.
            sysMdrUtil.findPaidAdIds4MdrAction(args0, limit = 1)
          }
          // Возможна NSEE, это нормально. Обходим проблемы совместимости NSEE с Vector через headOption.get (вместо head):
          nodeId = nodeIds.headOption.get
          mnodeOpt <- mNodesCache.getById( nodeId )
        } yield {
          // TODO Возможна ситуация, когда узел уже удалён, но в биллинге - ещё не модерирован. Эта ошибка в базах блокирует работу системы модерации.
          if (mnodeOpt.isEmpty) {
            // item + node_id есть, а узел отсутствует. Может быть кластер развалился, а может это ошибка в базе. Пока просто логгируем.
            // TODO Уведомлять юзера об ошибках в СУБД. На экране браузера отрендерить?
            LOGGER.error(s"$logPrefix node $nodeId missing, but present in items\n req=${request.uri}")
            throw new IllegalStateException( nodeId )
          } else {
            LOGGER.trace(s"$logPrefix mdr node => ${mnodeOpt.flatMap(_.id)} (${nodeIds.length})")
            mnodeOpt.get -> errNodeIdAcc
          }
        }

        mnodeFut.recoverWith { case ex: IllegalStateException =>
          val offset2 = args0.offset + 1
          LOGGER.trace(s"$logPrefix Will retry one more node [$offset2]")
          val nodeId = ex.getMessage
          val args2 = args0.copy(
            offsetOpt = Some( offset2 )
          )
          _findBillNode4MdrOrNseeFut( args2, nodeId :: errNodeIdAcc )
        }
      }

      // Поискать в биллинге узел, который надо модерировать:
      val billNodeWithErrorIdsOrNseeFut = _findBillNode4MdrOrNseeFut()
      val billedNodeOrExFut = billNodeWithErrorIdsOrNseeFut.map(_._1)

      // Если не будет найдено биллинга для модерации, то надо поискать бесплатные немодерированные размещения.
      val billedOrFreeNodeOrExFut = billedNodeOrExFut
        .recoverWith { case _: NoSuchElementException =>
          LOGGER.trace(s"$logPrefix No more paid advs, looking for free advs...\n $args")
          for {
            // Если нет paid-модерируемых карточек, то поискать бесплатные размещения.
            res <- mNodes.dynSearchOne(
              // TODO Добавить gen sort, где generation = args.gen, взятый например из personId.hashCode
              sysMdrUtil.freeMdrNodeSearchArgs(args, 1)
            )
          } yield {
            res.get
          }
        }

      // Нужно запустить сборку списков item'ов на модерацию.
      val mitemsFut = for {
        mdrNode <- billedOrFreeNodeOrExFut
        nodeId = mdrNode.id.get
        items <- slick.db.run {
          sysMdrUtil.itemsQueryAwaiting( nodeId )
            // Ограничиваем кол-во запрашиваемых item'ов. Нет никакого смысла вываливать слишком много данных на экран.
            .take(50)
            // Тяжелая сортировка тут скорее всего не важна, поэтому опускаем её.
            .result
        }
      } yield {
        LOGGER.trace(s"$logPrefix Found ${items.length} awaitingMdr-items for node#$nodeId")
        items
      }

      val ctx = implicitly[Context]

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
      val respFut = for {
        mdrNode      <- billedOrFreeNodeOrExFut

        errorNodeIds <- billNodeWithErrorIdsOrNseeFut
          .map(_._2.toSet)
          .recover { case _ => Nil }

        jdAdDataOpt <- jdAdDataSomeOrExFut
          .recover { case _: NoSuchElementException => None }

        mitems            <- mitemsFut
        selfRcvrsNeedMdr  <- selfRcvrsNeedMdrFut
        nodes             <- nodesRenderedFut
      } yield {
        val respNodeId = mdrNode.id.get
        LOGGER.trace(s"$logPrefix Done, returning mdr data for node#$respNodeId...")
        val resp = MNodeMdrInfo(
          nodeId  = respNodeId,
          ad      = jdAdDataOpt,
          items   = mitems,
          nodes   = nodes,
          directSelfNodeIds = selfRcvrsNeedMdr,
          errorNodeIds      = errorNodeIds,
        )
        Ok( Json.toJson(resp) )
      }

      // Не найдено узла? Это нормально, бывает.
      respFut.recover { case _: NoSuchElementException =>
        val msg = "No more nodes for moderation."
        LOGGER.trace(s"$logPrefix $msg")
        NoContent
      }
    }
  }


  /** Пришла команда от модератора об изменении состояния элементов модерации.
    *
    * @param mdrRes Контейнер данных по одному действию модерации.
    * @return
    */
  def doMdr(mdrRes: MMdrResolution) = csrf.Check {
    isSuNode(mdrRes.nodeId).async { implicit request =>
      // Надо организовать пакетное обновления в БД биллинга, в зависимости от значений полей резолюшена.
      for {
        _ <- sysMdrUtil.processMdrResolution( mdrRes, request.mnode, request.user )
      } yield {
        // Вернуть ответ -- обычно ничего возвращать не требуется.
        NoContent
      }
    }
  }

}

