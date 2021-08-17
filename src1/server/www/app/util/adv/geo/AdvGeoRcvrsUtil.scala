package util.adv.geo

import javax.inject.Inject
import akka.stream.scaladsl.Source
import controllers.routes
import io.suggest.adn.MAdnRights
import io.suggest.adv.geo.AdvGeoConstants
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.common.empty.OptionUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.common.geom.d2.MSize2di
import io.suggest.dev.MScreen
import io.suggest.es.model.{EsModel, IMust, MEsNestedSearch}
import io.suggest.geo.{IGeoShape, PointGs}
import io.suggest.maps.nodes.{MGeoNodePropsShapes, MRcvrsMapUrlArgs}
import io.suggest.media.{MMediaInfo, MMediaTypes}
import io.suggest.n2.edge.{MEdge, MEdgeInfo, MNodeEdges, MPredicates}
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.scripts.RcvrsMapNodesHashSumAggScripts
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.n2.node.{MNode, MNodes}
import io.suggest.playx.CacheApiUtil
import io.suggest.sc.ScConstants
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.util.JmxBase
import io.suggest.util.logs.MacroLogsImpl
import models.im.make.MImgMakeArgs
import models.im._
import models.mctx.Context
import play.api.mvc.Call
import util.adn.NodesUtil
import util.adv.build.AdvBuilderUtil
import util.adv.direct.AdvRcvrsUtil
import util.cdn.CdnUtil
import util.img.{DynImgUtil, FitImgMaker, LogoUtil, WelcomeUtil}
import japgolly.univeq._
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.11.16 15:01
  * Description: Утиль для форм размещения с гео-картам.
  */
final class AdvGeoRcvrsUtil @Inject()(
                                       injector: Injector,
                                     )
  extends MacroLogsImpl
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val logoUtil = injector.instanceOf[LogoUtil]
  private lazy val welcomeUtil = injector.instanceOf[WelcomeUtil]
  private lazy val advRcvrsUtil = injector.instanceOf[AdvRcvrsUtil]
  private lazy val nodesUtil = injector.instanceOf[NodesUtil]
  private lazy val cdnUtil = injector.instanceOf[CdnUtil]
  private lazy val dynImgUtil = injector.instanceOf[DynImgUtil]
  private lazy val fitImgMaker = injector.instanceOf[FitImgMaker]
  private lazy val cacheApiUtil = injector.instanceOf[CacheApiUtil]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  import esModel.api._


  /** "Версия" формата ресиверов, чтобы сбрасывать карту, даже когда она не изменилась. */
  private def RCVRS_MAP_CRC_VSN = 9

  /** Максимально допустимый уровень рекурсивного погружения во вложенность ресиверов.
    * Первый уровень -- это 1. */
  private def RCVR_TREE_MAX_DEEP = 4

  /**
    * Максимальное распараллелнивание при сборке логотипов для узлов.
    * Сборка логотипа в основном синхронная, поэтому можно распараллеливаться по-сильнее.
    */
  private def NODE_LOGOS_PREPARING_PARALLELISM = 16

  /** Предельные размеры логотипо в px. */
  private def LOGO_WH_LIMITS_CSSPX = MSize2di(width = 120, height = ScConstants.Logo.HEIGHT_CSSPX)


  /** Сборка ES-аргументов для поиска узлов, отображаемых на карте.
    * Этот набор критериев отбора используется и для сборки карты,
    * и для проверки прав на доступ к ресиверу, и последующих сопутствующих действий.
    *
    * @param limit1 Если ожидается source, то лимит за одну порцию.
    *               Если обычный поисковый запрос, то макс.размер единого ответа.
    * @param onlyWithIds Бывает, что интересуют только данные из указанного множества узлов.
    *                    Например, при сабмите надо проверить, всё ли ок.
    * @return Инстанс MNodeSearch.
    */
  def onMapRcvrsSearch(limit1: Int, onlyWithIds: Seq[String] = Nil): MNodeSearch = {
    new MNodeSearch {
      override val outEdges: MEsNestedSearch[Criteria] = {
        val crNodeLoc = Criteria(
          predicates  = MPredicates.NodeLocation :: Nil
        )
        MEsNestedSearch.plain( crNodeLoc )
      }
      override def isEnabled  = OptionUtil.SomeBool.someTrue
      override def testNode   = OptionUtil.SomeBool.someFalse
      // проверка типа узла на верхнем уровне НЕ нужна. MPredicate.AdnMap идёт через биллинг и надо подчиняться
      // воле финансов, даже если узел не очень Adn.
      override val withAdnRights = MAdnRights.RECEIVER :: Nil
      override def limit      = limit1
      // Фильтровать по id узлов, если таковые заданы... А такое тут часто.
      override def withIds    = onlyWithIds
    }
  }


  /** Запустить очень быстрый рассчёт хэш-суммы для карты ресиверов.
    * Лучше использовать rcvrNodesMapHashSumCached(), поэтому тут protected.
    *
    * Выполняется на стороне ES прямо на всех шардах.
    *
    * @return Целое число.
    */
  protected def rcvrNodesMapHashSum(): Future[Int] = {
    for {
      hashSum0 <- {
        val aggScripts = new RcvrsMapNodesHashSumAggScripts
        val query = onMapRcvrsSearch(0)
          .toEsQuery
        mNodes.docsHashSum( aggScripts, query )
      }
    } yield {
      // Тут костыль для "версии", чтобы сбрасывать некорректный кэш.
      // TODO Удалить этот .map после окончания отладки. А лучше унести куда-нибудь в Static-контроллер, т.к. номер версии может быть связан с форматом данных.
      hashSum0 + RCVRS_MAP_CRC_VSN
    }
  }
  /** Кэшируемое значение хэша rcvrs map. */
  def rcvrNodesMapHashSumCached(): Future[Int] = {
    import scala.concurrent.duration._
    cacheApiUtil.getOrElseFut("advRcvrsHash", expiration = 10.seconds) {
      rcvrNodesMapHashSum()
    }
  }

  def rcvrsMapUrlArgs()(implicit ctx: Context): Future[MRcvrsMapUrlArgs] = {
    for {
      hashSum <- rcvrNodesMapHashSumCached()
    } yield {
      MRcvrsMapUrlArgs( hashSum )
    }
  }

  /** Сборка правильной ссылки на на карту. */
  def rcvrNodesMapUrl()(implicit ctx: Context): Future[Call] = {
    for {
      nodesHashSum <- rcvrNodesMapHashSumCached()
    } yield {
      mkRcvrNodesMapUrl(nodesHashSum)(ctx)
    }
  }
  def mkRcvrNodesMapUrl(hashSum: Int)(implicit ctx: Context): Call = {
    cdnUtil.forCall(
      routes.Static.advRcvrsMapJson( hashSum )
    )(ctx)
  }

  /** Карта ресиверов, размещённых через lk-adn-map.
    *
    * @param nodesSrc Источник MNode. См. mNodes.source[MNode](...).
    * @return Source с данными по индексами узлов.
    */
  def nodesAdvGeoPropsSrc[M](nodesSrc: Source[MNode, M], wcAsLogo: Boolean = true): Source[(MNode, MSc3IndexResp), M] = {
    // TODO 2020-12-03 - Надо wcAsLogo устранить, и возвращать на клиент и welcome fg, и лого.
    //      Тут происходит подмена logoOpt на wcFg, невидимая на клиенте, что вызывает конфликты и нарушения в стилях SearchCss для одного и того же узла.

    // Начать выкачивать все подходящие узлы из модели:
    lazy val logPrefix = s"rcvrNodesMap(${System.currentTimeMillis}):"

    val targetScreenSome = Some( MScreen.default )

    val logoTargetSz = LOGO_WH_LIMITS_CSSPX

    val compressModeSome = Some(
      CompressModes.Fg
    )
    val logoSzMult = 1.0f
    val size2d_LENS = MSize2di.width

    // Пережевать все найденные узлы, собрать нужные данные в единый список.
    // Собрать логотипы узлов.
    nodesSrc
      .mapAsyncUnordered(NODE_LOGOS_PREPARING_PARALLELISM) { mnode =>
        // Подготовить инфу по логотипу узла.
        val mapLogoImgWithLimitsOptRaw = OptionUtil
          .maybeOpt(wcAsLogo) {
            welcomeUtil
              .wcFgImg(mnode)
          }
          .orElse {
            logoUtil
              .getLogoOfNode(mnode)
          }
          .map(_ -> logoTargetSz)

        if (mapLogoImgWithLimitsOptRaw.isEmpty)
          LOGGER.trace(s"$logPrefix Missing logo for node ${mnode.idOrNull}")

        val logoMakeResOptFut = FutureUtil.optFut2futOpt(mapLogoImgWithLimitsOptRaw) { case (logoRaw, targetSz) =>
          // Используем FitImgMaker, чтобы вписать лого в ограничения логотипа для этой карты.
          fitImgMaker
            .icompile {
              MImgMakeArgs(
                img           = logoRaw,
                targetSz      = targetSz,
                szMult        = logoSzMult,
                // На всех один и тот же экран, т.к. так быстрее. Логотипов десятки и сотни, и они мелкие, поэтому не важно.
                devScreenOpt  = targetScreenSome,
                compressMode  = compressModeSome
              )
            }
            .transform {
              case Success(logoMakeRes) =>
                LOGGER.trace(s"$logPrefix wh = ${logoMakeRes.szCss}csspx/${logoMakeRes.szReal}px for img $logoRaw fakeImgMake?${logoMakeRes.isFake}")
                val r = Some( logoMakeRes -> targetSz )
                Success(r)
              case Failure(ex) =>
                val msg = s"$logPrefix Node[${mnode.idOrNull}] with possible logo[$logoRaw] failed to prepare the logo for map"
                if (ex.isInstanceOf[NoSuchElementException] && !LOGGER.underlying.isTraceEnabled) {
                  LOGGER.warn(msg)
                } else {
                  LOGGER.warn(msg, ex)
                }
                Success( None )
            }
        }

        // Собираем props-константы за скобками, чтобы mnode-инстанс можно было "отпустить".
        val nodeId     = mnode.id.get
        val hintOpt    = mnode.guessDisplayName

        // Завернуть результат работы в итоговый контейнер, используемый вместо трейта.
        for {
          logoMakeResOpt <- logoMakeResOptFut
          logoOpt        = logoMakeResOpt.map(_._1.dynCallArgs)
          // Тут сборка MediaHostsMap для одного узла через пакетное API. Это не слишком эффективно, но в целом всё равно работает через кэш.
          mediaHostsMap  <- nodesUtil.nodeMediaHostsMap(
            logoImgOpt = logoOpt.toList
          )
        } yield {
          // Заверуть найденную иконку в пригодный для сериализации на клиент результат:
          val iconInfoOpt = for {
            (logoMakeRes, targetWh) <- logoMakeResOpt
          } yield {
            MMediaInfo(
              giType = MMediaTypes.Image,
              url    = dynImgUtil.distCdnImgCall( logoMakeRes.dynCallArgs, mediaHostsMap ).url,
              whPx   = {
                val szCss = logoMakeRes.szCss
                val szFinal = if (logoMakeRes.isFake) {
                  // Фейковый рендер, значит на выходе оригинальный размер. Надо спроецировать этот размер на targetWh по высоте:
                  size2d_LENS.set(
                    (szCss.width.toDouble / (szCss.height.toDouble / targetWh.height.toDouble)).toInt
                  )(targetWh)
                } else {
                  szCss
                }
                Some(szFinal)
              },
              contentType = logoMakeRes.dynCallArgs.dynImgId.imgFormat.get.mime,
            )
          }

          // props'ы для одной точки.
          val props = MSc3IndexResp(
            nodeId  = Some( nodeId ),
            ntype   = Some( mnode.common.ntype ),
            name    = hintOpt,
            // Цвета узла. Можно без цвета паттерна, т.к. он не нужен.
            colors  = mnode.meta.colors,
            logoOpt = iconInfoOpt
          )

          // Собрать и вернуть контейнер с данными мапы узлов:
          mnode -> props
        }
      }
  }

  /** Добавить гео-шейпы NodeLocation в src из nodesForVisualRenderSrc()
    *
    * @param src Результат nodesForVisualRenderSrc().
    * @tparam Mat Обычно NotUsed.
    * @return Source, выдающий ноды и MGeoNodePropsShapes.
    */
  def withNodeLocShapes[Mat]( src: Source[(MNode, MSc3IndexResp), Mat],
                              points: Boolean = true,
                              shapes: Boolean = false,
                            ): Source[(MNode, MGeoNodePropsShapes), Mat] = {
    src
      .map { case (mnode, props) =>
        // Собрать шейпы геолокации узла:
        val geoShapes = if (points || shapes) {
          (for {
            e <- mnode.edges.withPredicateIter( MPredicates.NodeLocation )
            gs <- {
              var acc = List.empty[IterableOnce[IGeoShape]]

              if (points) {
                acc ::= e.info.geoPoints
                  .iterator
                  .map( PointGs.apply )
              }

              if (shapes) {
                acc ::= e.info.geoShapes
                  .iterator
                  .map(_.shape)
              }

              acc
                .iterator
                .flatten
            }
          } yield {
            gs
          })
            .toSeq
        } else {
          Nil
        }

        val ngs = MGeoNodePropsShapes(
          props  = props,
          shapes = geoShapes
        )

        mnode -> ngs
      }
  }


  /** Проверка ресиверов, присланных клиентом.
    *
    * @param rcvrKeys Коллекция ключей из MFormS.rcvrsMap.keys/
    * @return Прочищенная коллекция RcvrKey, содержащая только выверенные элементы.
    */
  def checkRcvrs(rcvrKeys: Iterable[RcvrKey]): Future[Iterable[RcvrKey]] = {
    val topRcvrIdsSet = rcvrKeys
      .map(_.head)
      .toSet

    val topRcvrsCount = topRcvrIdsSet.size

    // Это валидируется на уровне AdvGeoFormUtil. Здесь просто проверка, что всё правильно, чисто на всякий случай.
    assert(topRcvrsCount <= AdvGeoConstants.AdnNodes.MAX_RCVRS_PER_TIME, "Too many rcvrs in map")
    assert(topRcvrsCount > 0, "Top rcvrs is empty")

    val idsSeq2Fut = mNodes.dynSearchIds {
      onMapRcvrsSearch( topRcvrsCount, topRcvrIdsSet.toSeq )
    }

    lazy val logPrefix = s"checkRcvrs(${rcvrKeys.size}/$topRcvrsCount)[${System.currentTimeMillis()}]:"

    LOGGER.trace(s"$logPrefix RcvrKeys0 = ${rcvrKeys.iterator.map(_.mkString(".")).mkString("[\n ", "\n ", "\n]")}")

    // Собрать множество очищенных ресиверов, т.е. ресиверов, в которых действительно можно размещаться...
    val rcvrKeys2Fut = for {
      idsSeq2 <- idsSeq2Fut
    } yield {
      val ids2 = idsSeq2.toSet
      val isOk = compareIdsSets(topRcvrIdsSet, ids2)

      // Получаем на руки обновлённый список на базе исходных rcvrKeys.
      // Почти всегда, он идентичен исходному, поэтому сравниваем множество FROM-ресиверов с исходным:
      val rcvrKeys2 = if (isOk) {
        LOGGER.trace(s"$logPrefix All FROM-rcvrs are allowed. Skipping FROM-filtering.")
        rcvrKeys
      } else {
        val rks2 = rcvrKeys.filter { rk =>
          ids2.contains(rk.head)
        }
        LOGGER.warn(s"$logPrefix There are ${rks2.size} top-nodes after re-filtering using cleared FROM rcvrs set: $rks2\n Was: ${rcvrKeys.size}: $rcvrKeys")
        rks2
      }

      rcvrKeys2
    }

    // Теперь надо проверить to-ресиверов (субресиверов), среди оставшихся данных по ресиверам.
    // Нужно поискать в базе суб-ресиверов, в которых можно размещаться.
    // Это может быть как размещение на самом ресивере, так и какие-то под-узлы (маячки, или что-то ещё...).
    rcvrKeys2Fut.flatMap { rcvrKeys2 =>
      if (rcvrKeys2.nonEmpty) {
        checkSubRcvrs(rcvrKeys2)
      } else {
        LOGGER.debug(s"$logPrefix No more rcvrs keys.")
        Future.successful( Nil )
      }
    }
  }


  /** Рекурсивная проверка доступа к ресиверам второго и последующих уровней.
    *
    * @param rcvrKeys Ключи ресиверов.
    *                 На нулевой позиции id над-ресиверов, которые считаются уже провренными и валидными.
    * @param currLevel Счётчик уровней погружения и одновременно итераций.
    * @return Выверенный список RcvrKey, где суб-ресиверы верны. Порядок считается неопределённым.
    */
  def checkSubRcvrs(rcvrKeys: Iterable[RcvrKey], currLevel: Int = 1): Future[Iterable[RcvrKey]] = {

    lazy val logPrefix = s"checkSubRcvrs(L$currLevel)[${System.currentTimeMillis()}]:"
    if (currLevel > RCVR_TREE_MAX_DEEP)
      throw new IllegalStateException(s"$logPrefix It's too deep, possibly erroneous recursion. Preventing SOE. Last data was:\n rcvrKeys0 = $rcvrKeys")

    // Пока проверяем просто пакетным поиском по базе на основе всех id и эджей в сторону родительских узлов.
    val (noChildRcvrKeys, hasChildRcvrKeys) = rcvrKeys.partition(_.tail.isEmpty)

    if (hasChildRcvrKeys.isEmpty) {
      LOGGER.trace(s"$logPrefix No more rcvrs with children. Rest = $noChildRcvrKeys")
      Future.successful(rcvrKeys)

    } else {
      // Собираем id родительских узлов:
      val parentIds = hasChildRcvrKeys.iterator
        .flatMap(_.headOption)
        .toSet

      // Собираем id дочерних узлов:
      val subIds = hasChildRcvrKeys.iterator
        .flatMap { _.tail.headOption }
        .toSet
      val subIdsSize = subIds.size

      LOGGER.trace(s"$logPrefix ${hasChildRcvrKeys.size}|p[${parentIds.size}|s$subIdsSize ,, RcvrKeys with children: $hasChildRcvrKeys")

      // Запихиваем всё в поисковый запрос для ES:
      val msearch = advRcvrsUtil.subRcvrsSearch(
        parentIds   = parentIds.toSeq,
        onlyWithIds = subIds.toSeq,
        limit1      = subIdsSize
      )

      // TODO Sec Мы тут не перепроверяем отношения parent-child. Просто пакетный ответ получаем, он нас устраивает.
      // Система сейчас не отрабатывает ситуацию, когда клиент переместил child id к другому parent'у.
      // Но в будущем возможно надо будет получать узлы полностью и перепроверять их по значению эджу OwnedBy.
      val subIds2Fut = mNodes.dynSearchIds(msearch)

      val rcvrKeys2Fut = for (subIdsSeq2 <- subIds2Fut) yield {
        val subIds2 = subIdsSeq2.toSet

        // Сравнить два множества суб-ресиверов. Сравнить их и сделать выводы...
        val isOk = compareIdsSets(subIds, subIds2)

        if (isOk) {
          LOGGER.trace(s"$logPrefix Sub-receivers matched ok.")
          hasChildRcvrKeys
        } else {
          val rks2 = hasChildRcvrKeys.filter { rk =>
            subIds2.contains( rk.tail.head )
          }
          LOGGER.warn(s"$logPrefix There are differences between expected and obtained receivers on level $currLevel.\n Source = $hasChildRcvrKeys\n Filtered = $rks2")
          rks2
        }
      }

      // Надо проверить под-ресиверы данных подресиверов, если таковые имеются.
      for {
        rcvrKeys2 <- rcvrKeys2Fut
        rcvrKeys3 <- {
          // Погружение на уровень вниз требует некоторых телодвижений: убрать одну голову у каждого RcvrKey, а потом вернуть её назад для результатов.
          val rcvrKeysGrpByParent = rcvrKeys2.groupBy(_.head)
          val currLevelPlus1 = currLevel + 1
          Future.traverse(rcvrKeysGrpByParent.toSeq) { case (parentId, pRcvrKeys) =>
            val cRcvrKeys = pRcvrKeys.map(_.tail)
            for (cRcvrKeys2 <- checkSubRcvrs( cRcvrKeys, currLevelPlus1 )) yield {
              for (crk <- cRcvrKeys2) yield {
                parentId :: crk : RcvrKey
              }
            }
          }.map { _.flatten }
        }
      } yield {
        // Закинуть исходные бездетные ресиверы в общую финальную кучу.
        noChildRcvrKeys ++ rcvrKeys3
      }
    }
  }

  /** Аккуратное сравнение множеств id ресиверов. */
  private def compareIdsSets(expected: Set[String], obtained: Set[String]): Boolean = {
    lazy val logPrefix = s"compareIdsSets(${expected.size},${obtained.size})[${System.currentTimeMillis()}]:"

    // Убедится, что код проверки работает корректно, и база не прислала левых id. Иначе, это неисправность какая-то.
    // Не может быть в ответе тех id, которые НЕ были запрошены при поиске.
    // revDiff-код -- это скорее тест-отладка, проверка самого себя. Но лучше пусть будет здесь, для самоконтроля нетривиальной системы.
    val revDiff = obtained -- expected
    if (revDiff.nonEmpty) {
      // should never happen: Что-то не так с алгоритмами или базой ES.
      LOGGER.error(
        logPrefix + " Something is going very wrong:\n" +
          s" Before FROM filtering was ${expected.size} node ids: $expected\n" +
          s" But after filtering there are ${obtained.size} ids: $obtained\n" +
          s" Rev.diff = $revDiff"
      )
      throw new IllegalAccessException(s"$logPrefix Rcvr nodes FROM-filtering working wrong. Cannot continue. See server logs.")
    }

    // Переходим от проверки самих себя к проверке результатов.
    // Определить наличие лишних элементов среди FROM-ресиверов:
    val isect = expected.intersect( obtained )
    if (isect.size != expected.size) {
      // Залогировать лишних FROM-ресиверов. Возможно, какой-то ресивер слетел с биллинга, пока юзер размещал что-то. А может ксакепы орудуют.
      LOGGER.warn {
        val diff = expected -- isect
        s"$logPrefix There are ${diff.size} excess FROM-rcvr ids: $diff . They are stripped out, possibly something was chagned during adv."
      }
    }

    // Получаем на руки обновлённый список на базе исходных rcvrKeys.
    // Почти всегда, он идентичен исходному, поэтому сравниваем множество FROM-ресиверов с исходным:
    isect ==* expected
  }


  /** Без-биллинговый апгрейд эджей захвата геолокации.
    * Есть много узлов, которые размещены на карте статично и вне биллинга.
    * Этот метод пересобирает узлы, добавляя название узла в эдж геолокации.
    *
    * @return Фьючерс с кол-вом обновлённых элементов.
    */
  def updateAllGeoLocEdgeTags(): Future[Int] = {
    // Инжектим инстанс, т.к. не нужен в остальном коде, а текущий метод - временный.
    lazy val advBuilderUtil = injector.instanceOf[AdvBuilderUtil]

    lazy val logPrefix = s"updateAllGeoLocEdgeTags()#${System.currentTimeMillis()}:"
    LOGGER.info(s"$logPrefix Starting...")

    val pred = MPredicates.NodeLocation

    val geoLocNodesSearch = new MNodeSearch {
      override val outEdges: MEsNestedSearch[Criteria] = {
        val cr = Criteria(
          predicates = pred :: Nil,
          must = IMust.MUST
        )
        MEsNestedSearch.plain( cr )
      }
    }

    mNodes.updateAll(
      scroller = mNodes.startScroll(
        queryOpt = geoLocNodesSearch.toEsQueryOpt
      )
    ) { mnode0 =>
      // Есть NodeLocation-эджи, которые требуют заполнения поля tags.
      val nodeLocEdgeTags = advBuilderUtil.nodeGeoLocTags(mnode0)

      val edge_info_tags_LENS = MEdge.info
        .composeLens( MEdgeInfo.tags )
      val mnode2 = MNode.edges
        .composeLens( MNodeEdges.out )
        .modify { edges0 =>
          for (e0 <- edges0) yield {
            if (e0.predicate ==>> pred) {
              // Апдейт tags
              (edge_info_tags_LENS set nodeLocEdgeTags)(e0)
            } else {
              e0
            }
          }
        }(mnode0)

      LOGGER.trace(s"$logPrefix Node#${mnode2.idOrNull} => tags=[${nodeLocEdgeTags.mkString(", ")}]")
      Future.successful( mnode2 )
    }
  }

}


/** Интерфейс для DI-поля с инстансом [[AdvGeoRcvrsUtil]]. */
trait IAdvGeoRcvrsUtilDi {
  def advGeoRcvrsUtil: AdvGeoRcvrsUtil
}



/** Интерфейс jmx mbean для [[AdvGeoRcvrsUtil]]. */
trait AdvGeoRcvrsUtilJmxMBean {

  /** Запуск чинилки поля tags в эдже NodeLocation. */
  def updateAllGeoLocEdges(): String

}

/** Реализация JMX для [[AdvGeoRcvrsUtil]]. */
final class AdvGeoRcvrsUtilJmx @Inject() (
                                           advGeoRcvrsUtil: AdvGeoRcvrsUtil,
                                           implicit val ec: ExecutionContext
                                         )
  extends JmxBase
  with AdvGeoRcvrsUtilJmxMBean
{

  import io.suggest.util.JmxBase._

  override def _jmxType = Types.UTIL

  override def updateAllGeoLocEdges(): String = {
    val fut = for {
      countUpdated <- advGeoRcvrsUtil.updateAllGeoLocEdgeTags()
    } yield {
      s"Done, ${countUpdated} nodes updated."
    }
    awaitString(fut)
  }

}
