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
import io.suggest.es.model.IMust
import io.suggest.maps.nodes.{MAdvGeoMapNodeProps, MGeoNodePropsShapes, MMapNodeIconInfo, MRcvrsMapUrlArgs}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.scripts.RcvrsMapNodesHashSumAggScripts
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import io.suggest.model.n2.node.{MNode, MNodes}
import io.suggest.sc.ScConstants
import io.suggest.util.JMXBase
import io.suggest.util.logs.MacroLogsImpl
import models.im.make.MImgMakeArgs
import models.im._
import models.mctx.Context
import models.mproj.ICommonDi
import play.api.mvc.Call
import util.adn.NodesUtil
import util.adv.build.AdvBuilderUtil
import util.adv.direct.AdvRcvrsUtil
import util.cdn.CdnUtil
import util.img.{DynImgUtil, FitImgMaker, LogoUtil, WelcomeUtil}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.11.16 15:01
  * Description: Утиль для форм размещения с гео-картам.
  */
class AdvGeoRcvrsUtil @Inject()(
                                 mNodes      : MNodes,
                                 logoUtil    : LogoUtil,
                                 welcomeUtil : WelcomeUtil,
                                 advRcvrsUtil: AdvRcvrsUtil,
                                 nodesUtil   : NodesUtil,
                                 cdnUtil     : CdnUtil,
                                 mAnyImgs    : MAnyImgs,
                                 dynImgUtil  : DynImgUtil,
                                 fitImgMaker : FitImgMaker,
                                 mCommonDi   : ICommonDi
                               )
  extends MacroLogsImpl
{

  import mCommonDi._


  /** "Версия" формата ресиверов, чтобы сбрасывать карту, даже когда она не изменилась. */
  private def RCVRS_MAP_CRC_VSN = 7

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
    new MNodeSearchDfltImpl {
      override def outEdges: Seq[Criteria] = {
        val crNodeLoc = Criteria(
          predicates  = MPredicates.NodeLocation :: Nil
        )
        crNodeLoc :: Nil
      }
      override def isEnabled  = Some(true)
      override def testNode   = Some(false)
      // проверка типа узла на верхнем уровне НЕ нужна. MPredicate.AdnMap идёт через биллинг и надо подчиняться
      // воле финансов, даже если узел не очень Adn.
      override def withAdnRights = MAdnRights.RECEIVER :: Nil
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
        /*
        val aggScripts = FieldsHashSumsAggScripts(
          sourceFields = List(
            // Эджи. Там array, поэтому дальше погружаться нельзя. TODO А интересуют только эджи захвата геолокации и логотипа узла
            MNodeFields.Edges.E_OUT_FN,
            // Название узла тоже интересует. Но его может и не быть, поэтому интересуемся только контейнер meta.basic, который есть всегда
            MNodeFields.Meta.META_BASIC_FN,
            MNodeFields.Meta.META_COLORS_FN
          )
        )
        */
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
  def rcvrNodesMapHashSumCached(): Future[Int] = {
    import scala.concurrent.duration._
    cacheApiUtil.getOrElseFut("advRcvrsHash", expiration = 10.seconds) {
      rcvrNodesMapHashSum()
    }
  }

  def rcvrsMapUrlArgs()(implicit ctx: Context): Future[MRcvrsMapUrlArgs] = {
    val hashSumFut = rcvrNodesMapHashSumCached()
    for {
      hashSum <- hashSumFut
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
    * @param msearch Источник MNode. См. mNodes.source[MNode](...).
    * @return Фьючерс с картой узлов.
    *         Карта нужна для удобства кэширования и как бы "сортировки", чтобы hashCode() или иные хэш-функции
    *         всегда возвращали один и тот же результат.
    */
  def nodesAdvGeoPropsSrc[M](nodesSrc: Source[MNode, M], wcAsLogo: Boolean): Source[(MNode, MAdvGeoMapNodeProps), M] = {
    // Начать выкачивать все подходящие узлы из модели:
    lazy val logPrefix = s"rcvrNodesMap(${System.currentTimeMillis}):"

    val targetScreenSome = Some( MScreen.default )

    val logoTargetSz = LOGO_WH_LIMITS_CSSPX

    val compressModeSome = Some(
      CompressModes.Fg
    )
    val logoSzMult = 1.0f

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
          val imakeArgs = MImgMakeArgs(
            img           = logoRaw,
            targetSz      = targetSz,
            szMult        = logoSzMult,
            // На всех один и тот же экран, т.к. так быстрее. Логотипов десятки и сотни, и они мелкие, поэтому не важно.
            devScreenOpt  = targetScreenSome,
            compressMode  = compressModeSome
          )
          val fut = for {
            logoMakeRes <- fitImgMaker.icompile( imakeArgs )
          } yield {
            LOGGER.trace(s"$logPrefix wh = ${logoMakeRes.szCss}csspx/${logoMakeRes.szReal}px for img $logoRaw")
            Some( logoMakeRes -> targetSz )
          }

          // Подавлять ошибки рендера логотипа. Дефолтовщины хватит, главное чтобы всё было ок.
          fut.recover { case ex: Throwable =>
            val msg = s"$logPrefix Node[${mnode.idOrNull}] with possible logo[$logoRaw] failed to prepare the logo for map"
            if (ex.isInstanceOf[NoSuchElementException] && !LOGGER.underlying.isTraceEnabled) {
              LOGGER.warn(msg)
            } else {
              LOGGER.warn(msg, ex)
            }
            None
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
          mediaHostsMap  <- nodesUtil.nodeMediaHostsMap( logoImgOpt = logoOpt )
        } yield {
          // Заверуть найденную иконку в пригодный для сериализации на клиент результат:
          val iconInfoOpt = for {
            (logoMakeRes, targetWh) <- logoMakeResOpt
          } yield {
            MMapNodeIconInfo(
              url = dynImgUtil.distCdnImgCall( logoMakeRes.dynCallArgs, mediaHostsMap ).url,
              wh  = {
                val szCss = logoMakeRes.szCss
                if (logoMakeRes.isFake) {
                  // Фейковый рендер, значит на выходе оригинальный размер. Надо спроецировать этот размер на targetWh по высоте:
                  targetWh.withWidth(
                    (szCss.width.toDouble / (szCss.height.toDouble / targetWh.height.toDouble)).toInt
                  )
                } else {
                  szCss
                }
              }
            )
          }

          // props'ы для одной точки.
          val props = MAdvGeoMapNodeProps(
            nodeId  = nodeId,
            ntype   = mnode.common.ntype,
            hint    = hintOpt,
            // Цвета узла. Можно без цвета паттерна, т.к. он не нужен.
            colors  = mnode.meta.colors,
            icon    = iconInfoOpt
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
  def withNodeLocShapes[Mat]( src: Source[(MNode, MAdvGeoMapNodeProps), Mat] ): Source[(MNode, MGeoNodePropsShapes), Mat] = {
    src
      .map { case (mnode, props) =>
        // Собрать шейпы геолокации узла:
        val geoShapes = mnode.edges
          .withPredicateIter( MPredicates.NodeLocation )
          .flatMap( _.info.geoShapes )
          .map(_.shape)
          .toSeq

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
    isect == expected
  }


  /** Без-биллинговый апгрейд эджей захвата геолокации.
    * Есть много узлов, которые размещены на карте статично и вне биллинга.
    * Этот метод пересобирает узлы, добавляя название узла в эдж геолокации.
    *
    * @return Фьючерс с кол-вом обновлённых элементов.
    */
  def updateAllGeoLocEdgeTags(): Future[Int] = {
    // Инжектим инстанс, т.к. не нужен в остальном коде, а текущий метод - временный.
    val inj = mCommonDi.current.injector
    lazy val advBuilderUtil = inj.instanceOf[AdvBuilderUtil]

    lazy val logPrefix = s"updateAllGeoLocEdgeTags()#${System.currentTimeMillis()}:"
    LOGGER.info(s"$logPrefix Starting...")

    val pred = MPredicates.NodeLocation

    val geoLocNodesSearch = new MNodeSearchDfltImpl {
      override def outEdges: Seq[Criteria] = {
        val cr = Criteria(
          predicates = pred :: Nil,
          must = IMust.MUST
        )
        cr :: Nil
      }
    }

    mNodes.updateAll(
      scroller = mNodes.startScroll(
        queryOpt = geoLocNodesSearch.toEsQueryOpt
      )
    ) { mnode0 =>
      // Есть NodeLocation-эджи, которые требуют заполнения поля tags.
      val nodeLocEdgeTags = advBuilderUtil.nodeGeoLocTags(mnode0)
      val mnode2 = mnode0.withEdges(
        mnode0.edges.withOut(
          for (e0 <- mnode0.edges.out) yield {
            if (e0.predicate ==>> pred) {
              // Апдейт tags
              e0.withInfo(
                e0.info.withTags( nodeLocEdgeTags )
              )
            } else {
              e0
            }
          }
        )
      )
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
class AdvGeoRcvrsUtilJmx @Inject() (
                                     advGeoRcvrsUtil: AdvGeoRcvrsUtil,
                                     override implicit val ec: ExecutionContext
                                   )
  extends JMXBase
  with AdvGeoRcvrsUtilJmxMBean
  with MacroLogsImpl
{

  override def jmxName = "io.suggest:type=util,name=" + getClass.getSimpleName.replace("Jmx", "")

  override def updateAllGeoLocEdges(): String = {
    val fut = for {
      countUpdated <- advGeoRcvrsUtil.updateAllGeoLocEdgeTags()
    } yield {
      s"Done, ${countUpdated} nodes updated."
    }
    awaitString(fut)
  }

}
