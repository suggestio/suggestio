package util.adv.geo

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import io.suggest.adv.geo.AdvGeoConstants
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.async.StreamsUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.{MNode, MNodes}
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.ym.model.common.AdnRights
import models.ISize2di
import models.adv.geo.mapf.{MAdvGeoMapNode, MAdvGeoMapNodeProps, MIconInfo}
import models.im.{MAnyImgs, MImgT}
import models.mctx.Context
import models.mproj.ICommonDi
import util.cdn.CdnUtil
import util.img.{DynImgUtil, LogoUtil}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.11.16 15:01
  * Description:
  */
class AdvGeoMapUtil @Inject() (
                                mNodes      : MNodes,
                                logoUtil    : LogoUtil,
                                cdnUtil     : CdnUtil,
                                mAnyImgs    : MAnyImgs,
                                dynImgUtil  : DynImgUtil,
                                streamsUtil : StreamsUtil,
                                mCommonDi   : ICommonDi
)
  extends MacroLogsImpl
{

  import mCommonDi._
  import mNodes.Implicits._

  /** Максимально допустимый уровень рекурсивного погружения во вложенность ресиверов.
    * Первый уровень -- это 1. */
  private def RCVR_TREE_MAX_DEEP = 4

  /**
    * Максимальное распараллелнивание при сборке логотипов для узлов.
    * Сборка логотипа в основном синхронная, поэтому можно распараллеливаться по-сильнее.
    */
  private def NODE_LOGOS_PREPARING_PARALLELISM = 16

  /** Размер логотипа (по высоте) на карте. */
  private val LOGO_HEIGHT_CSSPX = configuration.getInt("node.logo.on.map.height.px").getOrElse(20)


  private case class LogoInfo(logo: MImgT, wh: ISize2di)
  private case class NodeInfo(mnode: MNode, logoInfoOpt: Option[LogoInfo])


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
      override def outEdges: Seq[ICriteria] = {
        val cr = Criteria(
          predicates = MPredicates.AdnMap :: Nil
        )
        Seq(cr)
      }
      override def isEnabled  = Some(true)
      override def testNode   = Some(false)
      // проверка типа узла на верхнем уровне НЕ нужна. MPredicate.AdnMap идёт через биллинг и надо подчиняться
      // воле финансов, даже если узел не очень Adn.
      override def withAdnRights = AdnRights.RECEIVER :: Nil
      override def limit      = limit1
      // Фильтровать по id узлов, если таковые заданы... А такое тут часто.
      override def withIds    = onlyWithIds
    }
  }


  /** Карта ресиверов, размещённых через lk-adn-map.
    *
    * @param msearch Инстас поисковых аргументов, собранный через rcvrsSearch().
    * @return Фьючерс с GeoJSON.
    */
  def rcvrNodesMap(msearch: MNodeSearch)(implicit ctx: Context): Source[MAdvGeoMapNode, NotUsed] = {
    // Начать выкачивать все подходящие узлы из модели:
    val nodesSource = mNodes.source[MNode](msearch)
    // TODO Opt направить этот поток в кэш узлов MNodeCache?

    lazy val logPrefix = s"rcvrNodesMap(${System.currentTimeMillis}):"

    val logosAndNodeSrc = nodesSource.mapAsyncUnordered(NODE_LOGOS_PREPARING_PARALLELISM) { mnode =>
      val logoInfoOptFut = logoUtil.getLogoOfNode(mnode).flatMap { logoOptRaw =>
        FutureUtil.optFut2futOpt(logoOptRaw) { logoRaw =>
          val fut = for {
            logo      <- logoUtil.getLogo4scr(logoRaw, LOGO_HEIGHT_CSSPX, None)
            localImg  <- dynImgUtil.ensureImgReady(logo, cacheResult = true)
            whOpt     <- mAnyImgs.getImageWH(localImg)
          } yield {
            whOpt.fold[Option[LogoInfo]] {
              LOGGER.warn(s"Unable to fetch WH of logo $logo for node ${mnode.idOrNull}")
              None
            } { wh =>
              Some(LogoInfo(logo, wh))
            }
          }

          // Подавлять ошибки рендера логотипа. Дефолтовщины хватит, главное чтобы всё было ок.
          fut.recover { case ex: Throwable =>
            LOGGER.error(s"$logPrefix Node[${mnode.idOrNull}] with possible logo[$logoRaw] failed to prepare the logo for map", ex)
            None
          }
        }
      }

      // Завернуть результат работы в итоговый контейнер, используемый вместо трейта.
      for (logoInfoOpt <- logoInfoOptFut) yield {
        NodeInfo(mnode, logoInfoOpt)
      }
    }

    // Отмаппить узлы в представление, годное для GeoJSON-сериализации. Финальную сериализацию организует контроллер.
    val resultsSrc = logosAndNodeSrc.mapConcat { nodeInfo =>
      // Собираем url отдельно и ровно один раз, чтобы сэкономить ресурсы.
      val iconInfoOpt = for {
        logoInfo <- nodeInfo.logoInfoOpt
      } yield {
        MIconInfo(
          url    = cdnUtil.dynImg(logoInfo.logo).url,
          width  = logoInfo.wh.width,
          height = logoInfo.wh.height
        )
      }
      val mnode = nodeInfo.mnode

      val props = MAdvGeoMapNodeProps(
        nodeId  = mnode.id.get,
        hint    = mnode.guessDisplayName,
        bgColor = mnode.meta.colors.bg
          .map(_.code),
        icon    = iconInfoOpt
      )

      mnode.edges
        .withPredicateIter( MPredicates.AdnMap )
        .flatMap( _.info.geoPoints )
        .map { geoPoint =>
          MAdvGeoMapNode(
            point = geoPoint,
            props = props
          )
        }
        .toStream // Это типа toImmutableIterable
    }

    // Залоггировать общее кол-во отправленных наверх нод, если включена трассировка.
    streamsUtil.maybeTraceCount(resultsSrc, this) { total =>
      s"$logPrefix Returned $total map-nodes."
    }

    resultsSrc
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
      val msearch = subRcvrsSearch(
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

  /** Поиск под-ресиверов по отношению к указанным ресиверам. */
  def subRcvrsSearch(parentIds: Seq[String], onlyWithIds: Seq[String] = Nil, limit1: Int = 100 ): MNodeSearch = {
    new MNodeSearchDfltImpl {
      override def isEnabled = Some(true)
      override def outEdges: Seq[ICriteria] = {
        val cr = Criteria(
          nodeIds    = parentIds,
          predicates = MPredicates.OwnedBy :: Nil
        )
        cr :: Nil
      }
      override def withAdnRights  = AdnRights.RECEIVER :: Nil
      override def limit          = limit1
      override def withIds        = onlyWithIds
    }
  }

  /** Поиск под-ресиверов у указанного ресивера. */
  def findSubRcvrsOf(rcvrId: String): Future[Seq[MNode]] = {
    val search = subRcvrsSearch(parentIds = rcvrId :: Nil)
    mNodes.dynSearch(search)
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

}


/** Интерфейс для DI-поля с инстансом [[AdvGeoMapUtil]]. */
trait IAdvGeoMapUtilDi {
  def advGeoMapUtil: AdvGeoMapUtil
}
