package util.adn

import java.time.OffsetDateTime

import akka.stream.scaladsl.Sink
import javax.inject.{Inject, Singleton}
import controllers.routes
import io.suggest.adn.MAdnRights
import io.suggest.common.coll.Lists.Implicits._
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.edge.{MEdge, MNodeEdges, MPredicates}
import io.suggest.model.n2.extra.{MAdnExtra, MNodeExtras}
import io.suggest.model.n2.media.MMediasCache
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import io.suggest.url.MHostInfo
import io.suggest.util.logs.MacroLogsImpl
import models.AdnShownTypes
import models.adv.MExtTargets
import models.im.MImgT
import models.madn.{MNodeRegSuccess, NodeDfltColors}
import models.mext.MExtServices
import models.mproj.ICommonDi
import models.mwc.MWelcomeRenderArgs
import play.api.i18n.Messages
import play.api.mvc.Call
import util.cdn.CdnUtil
import util.img.DynImgUtil

import scala.concurrent.Future
import scala.util.Random

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.02.15 9:53
 * Description: Утиль для работы с нодами. Появилось, когда понадобилась функция создания пользовательского узла
 * в нескольких контроллерах.
 * 2015.mar.18: Для новосозданного узла нужно создавать начальные рекламные карточки.
 */
@Singleton
final class NodesUtil @Inject() (
                                  mNodes                  : MNodes,
                                  mExtTargets             : MExtTargets,
                                  mMediasCache            : MMediasCache,
                                  cdnUtil                 : CdnUtil,
                                  dynImgUtil              : DynImgUtil,
                                  mCommonDi               : ICommonDi
                                )
  extends MacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  /** Через сколько секунд отправлять юзера в ЛК ноды после завершения реги юзера. */
  private def NODE_CREATED_SUCCESS_RDR_AFTER: Int = 5

  // Для новосозданного узла надо создавать новые карточки, испортируя их из указанного узла в указанном кол-ве.
  /** id узла, который содержит дефолтовые карточки. Задается явно в конфиге. */
  def ADN_IDS_INIT_ADS_SOURCE = Nil

  /** Кол-во карточек для импорта из дефолтового узла. */
  def INIT_ADS_COUNT = 1


  /** Куда отправлять юзера, когда тот создал новый узел? */
  def userNodeCreatedRedirect(adnId: String): Call = {
    routes.LkAdnEdit.editNodePage( adnId )
  }

  /** Для рендера шаблона regSuccessTpl требуется собрать аргументы для рендера. */
  def nodeRegSuccessArgs(mnode: MNode): MNodeRegSuccess = {
    MNodeRegSuccess(
      mnode,
      userNodeCreatedRedirect( mnode.id.get ),
      NODE_CREATED_SUCCESS_RDR_AFTER
    )
  }


  /** Собрать критерии поиска узлов, прямо принадлежащих текущему юзеру. */
  def personNodesSearch(personId: String, limit1: Int = 200, withoutIds1: Seq[String] = Nil): MNodeSearch = {
    new MNodeSearchDfltImpl {
      override def outEdges = Seq(
        Criteria(
          nodeIds     = personId :: Nil,
          predicates  = MPredicates.OwnedBy :: Nil
        )
      )
      override def limit      = limit1
      override def withoutIds = withoutIds1
    }
  }


  /**
    * Создать новый инстанс узла для юзера без сохранения узла в хранилище.
    *
    * @param nameOpt название узла.
    * @param personIdOpt id юзера-владельца.
    * @return Экземпляр узла без id.
    */
  def userNodeInstance(nameOpt: Option[String], personIdOpt: Option[String]): MNode = {
    MNode(
      common = MNodeCommon(
        ntype = MNodeTypes.AdnNode,
        isDependent = true
      ),
      meta = MMeta(
        basic = MBasicMeta(
          nameOpt = nameOpt
        ),
        colors = NodeDfltColors.getOneRandom().adnColors
      ),
      extras = MNodeExtras(
        adn =  Some(MAdnExtra(
          rights          = Set(MAdnRights.PRODUCER, MAdnRights.RECEIVER),
          isUser          = true,
          shownTypeIdOpt  = Some(AdnShownTypes.SHOP.name),
          testNode        = false,
          showInScNl      = false
        ))
      ),
      edges = MNodeEdges(
        out = {
          personIdOpt.fold[Seq[MEdge]] (Nil) { personId =>
            val medge = MEdge(
              predicate = MPredicates.OwnedBy,
              nodeIds   = Set(personId)
            )
            MNodeEdges.edgesToMap(medge)
          }
        }
      )
    )
  }

  /**
    * Создать дефолтовые таргеты для размещения в соц.сетях.
    *
    * @param adnId id узла.
    * @return Фьючерс для синхронизации.
    */
  def createExtDfltTargets(adnId: String)(implicit messages: Messages): Future[_] = {
    val tgtsIter = MExtServices.values
      .iterator
      .flatMap { svc => mExtTargets.dfltTarget(svc, adnId) }
    Future.traverse(tgtsIter)(mExtTargets.save)
  }

  /**
    * Создание нового узла для юзера. Узел должен быть готов к финансовой работе.
    *
    * @param name Название узла.
    * @param personId id юзера-владельца.
    * @return Фьючерс с готовым инстансом нового существующего узла.
    */
  def createUserNode(name: String, personId: String)(implicit messages: Messages): Future[MNode] = {
    val inst = userNodeInstance(
      nameOpt     = Some(name),
      personIdOpt = Some(personId)
    )
    for {
      nodeId        <- mNodes.save(inst)
      // TODO Нужно инсталлить jd-карточки, а не этот старый хлам
      //madsCreateFut = installDfltMads(nodeId)
      _             <- createExtDfltTargets(nodeId)
      //_             <- madsCreateFut
    } yield {
      inst.copy(id = Some(nodeId))
    }
  }


  /** Установить дефолтовые карточки. */
  def installDfltMads(adnId: String, count: Int = INIT_ADS_COUNT)(implicit messages: Messages): Future[Seq[String]] = {
    lazy val logPrefix = s"installDfltMads($adnId):"
    (Future successful ADN_IDS_INIT_ADS_SOURCE)
      // Если нет продьюсеров, значит функция отключена. Это будет перехвачено в recover()
      .filter { _.nonEmpty }

      // Собрать id карточек, относящиеся к заданным узлам-источникам.
      .flatMap { prodIds =>
        val _limit = Math.max(50, count * 2)
        val dsa0 = new MNodeSearchDfltImpl {
          override def nodeTypes = Seq( MNodeTypes.Ad )
          override def outEdges: Seq[Criteria] = {
            val cr = Criteria(
              nodeIds     = prodIds,
              predicates  = Seq( MPredicates.OwnedBy )
            )
            Seq(cr)
          }
          override def limit  = _limit
          override def offset = 0
        }
        mNodes.dynSearchIds(dsa0)
      }

      // Случайно выбрать из списка id карточек только указанное кол-во карточек.
      .flatMap { madIdsRes =>
        val count = madIdsRes.length
        val rnd = new Random()
        val madIds2 = for {
          _ <- (0 until Math.min(count, count)).iterator
        } yield {
          madIdsRes( rnd.nextInt(count) )
        }
        mNodes.multiGet(madIds2)
      }

      // Обновить карточки
      .flatMap { mads0 =>
        trace(s"$logPrefix Will install ${mads0.size} ads: [${mads0.iterator.flatMap(_.id).mkString(", ")}]")

        Future.traverse(mads0) { mad0 =>
          // Создать новую карточку на базе текущей.
          val mad1 = mad0.copy(
            id = None,
            versionOpt = None,
            meta = mad0.meta.copy(
              basic = mad0.meta.basic.copy(
                dateCreated = OffsetDateTime.now(),
                dateEdited  = None
              )
            ),
            edges = MNodeEdges(
              out = {
                val pp = MPredicates.OwnedBy
                val rp = MPredicates.Receiver.Self
                val nodeIdSet = Set(adnId)
                val prodE = MEdge(
                  predicate = pp,
                  nodeIds   = nodeIdSet
                )
                val selfRcvrE = MEdge(
                  predicate = rp,
                  nodeIds   = nodeIdSet
                )
                MNodeEdges.edgesToMap1 {
                  mad0.edges
                    .withoutPredicateIter(pp, rp)
                    .++( prodE :: selfRcvrE :: Nil )
                }
              }
            )
          )

          // Запустить сохранение сгенеренной карточки.
          mNodes.save(mad1)
        }
      }

      // Если не было adnId узлов-источников, то
      .recover { case _: NoSuchElementException =>
        LOGGER.warn(logPrefix + " Node default ads installer is disabled!")
        Nil
      }
  }


  /** Подготовка MediaHostsMap для рендера медиа-файлов узла с использование dist-cdn.
    * Изначально код зародился в выдаче, но вынесен для возможности использования на других страницах узла.
    *
    * @param logoImgOpt Инфа по картинке-логотипу.
    *                   см. [[util.img.LogoUtil.getLogoOfNode()]] и [[util.img.LogoUtil.getLogoOpt4scr()]].
    *                   или [[util.img.FitImgMaker]].
    * @param welcomeOpt Инфа по welcome-картинкам.
    *                   см. [[util.img.WelcomeUtil.getWelcomeRenderArgs()]]
    * @param gallery Инфа по внутренней галлерее картинок узла.
    *                см. [[util.img.GalleryUtil.galleryImgs()]]
    * @return Выхлоп [[util.cdn.CdnUtil.mediasHosts()]].
    */
  def nodeMediaHostsMap(logoImgOpt: Option[MImgT]               = None,
                        welcomeOpt: Option[MWelcomeRenderArgs]  = None,
                        gallery   : List[MImgT]                 = Nil
                       ): Future[Map[String, Seq[MHostInfo]]] = {
    var allImgsAcc = gallery

    allImgsAcc = welcomeOpt
      .iterator
      .flatMap( _.allImgsWithWhInfoIter )
      .map(_.mimg)
      .prependRevTo( allImgsAcc )

    allImgsAcc = logoImgOpt.prependTo(allImgsAcc)

    // Запустить сборку MediaHostsMap.
    dynImgUtil.mkMediaHostsMap( allImgsAcc )
  }


  /** Сбор всех дочерних id в одну кучу по OwnedBy предикату.
    *
    * @param parentNodeIds Интересующие id родительских узлов.
    * @param maxLevels Максимальное кол-во шагов.
    * @return Фьючерс с множеством дочерних узлов + parentNodeIds.
    */
  def collectChildIds(parentNodeIds: Set[String], maxLevels: Int = 3, perStepLimit: Int = 100): Future[Set[String]] = {
    val _predicates = MPredicates.OwnedBy :: Nil

    // Асинхронная рекурсия поиска id-узлов с погружением на под-уровни.
    def __fold(parentNodeIdsCurrent: Set[String], acc0Fut: Future[Set[String]], counter: Int): Future[Set[String]] = {
      if (counter >= maxLevels || parentNodeIdsCurrent.isEmpty) {
        acc0Fut

      } else {
        mNodes
          .dynSearchIds {
            val crs = Criteria(
              nodeIds     = parentNodeIdsCurrent.toSeq,
              predicates  = _predicates
            ) :: Nil
            new MNodeSearchDfltImpl {
              override def outEdges = crs
              override def limit = perStepLimit
            }
          }
          .flatMap { idsResp =>
            val idsSet = idsResp.toSet
            acc0Fut.flatMap { acc0 =>
              val acc2Fut = Future( acc0 ++ idsSet )

              // Чтобы не было циклов графа, надо выкинуть id уже пройденных узлов. В норме - тут просто пересборка idsSet.
              val alreadyUsedIds = idsSet intersect acc0

              // Акк уже готов, но нужно собрать список родительских узлов для следующей итерации:
              val idsSet2 =
                if (alreadyUsedIds.isEmpty) idsSet
                else idsSet -- alreadyUsedIds

              // И перейти на следующую итерацию:
              __fold(idsSet2, acc2Fut, counter + 1)
            }
          }
      }
    }

    // Запуск рекурсии.
    __fold(
      parentNodeIdsCurrent  = parentNodeIds,
      acc0Fut               = Future.successful( parentNodeIds ),
      counter               = 0
    )
  }


  /** Собрать id родительских узлов по отношению к указанным дочерним узлам. */
  def directOwnerIdsOf(childrenIds: Iterable[String]): Future[Set[String]] = {
    // Чтобы узнать родительские узлы, надо прочитать текущие узлы:
    mNodesCache
      .multiGetSrc( childrenIds )
      .mapConcat { mnode =>
        mnode.edges
          .withPredicateIterIds( MPredicates.OwnedBy )
          .toSet
      }
      .runWith( Sink.collection[String, Set[String]] )
  }

}


/** Интерфейс для DI-поля, содержащего инжектируемый экземпляр [[util.adn.NodesUtil]]. */
trait INodesUtil {
  def nodesUtil: NodesUtil
}
