package util.adn

import java.time.OffsetDateTime
import javax.inject.{Inject, Singleton}

import controllers.routes
import io.suggest.adn.MAdnRights
import io.suggest.color.MColorData
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.edge.{MEdge, MNodeEdges, MPredicates}
import io.suggest.model.n2.extra.{MAdnExtra, MNodeExtras}
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.model.n2.node.meta.colors.MColors
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import io.suggest.util.logs.MacroLogsImpl
import models.AdnShownTypes
import models.adv.MExtTargets
import models.madn.{MNodeRegSuccess, NodeDfltColors}
import models.mext.MExtServices
import models.mproj.ICommonDi
import play.api.i18n.Messages
import play.api.mvc.Call

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
class NodesUtil @Inject() (
  mNodes                  : MNodes,
  mExtTargets             : MExtTargets,
  mCommonDi               : ICommonDi
)
  extends MacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  /** Через сколько секунд отправлять юзера в ЛК ноды после завершения реги юзера. */
  private val NODE_CREATED_SUCCESS_RDR_AFTER: Int = configuration.getOptional[Int]("user.node.created.success.redirect.after.sec")
    .getOrElse(5)

  // Для новосозданного узла надо создавать новые карточки, испортируя их из указанного узла в указанном кол-ве.
  /** id узла, который содержит дефолтовые карточки. Задается явно в конфиге. */
  val ADN_IDS_INIT_ADS_SOURCE = configuration.getOptional[Seq[String]]("user.node.created.mads.import.from.adn_ids")
    .getOrElse(Nil)

  /** Кол-во карточек для импорта из дефолтового узла. */
  val INIT_ADS_COUNT = configuration.getOptional[Int]("user.node.created.mads.import.count")
    .getOrElse(1)


  /** Куда отправлять юзера, когда тот создал новый узел? */
  def userNodeCreatedRedirect(adnId: String): Call = {
    routes.MarketLkAdnEdit.editAdnNode(adnId)
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
  def personNodesSearch(personId: String, limit1: Int = 100, withoutIds1: Seq[String] = Nil): MNodeSearch = {
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
        colors = {
          val dc = NodeDfltColors.getOneRandom()
          MColors(
            bg = Some( MColorData(dc.bgColor) ),
            fg = Some( MColorData(dc.fgColor) )
          )
        }
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
      madsCreateFut = installDfltMads(nodeId)
      _             <- createExtDfltTargets(nodeId)
      _             <- madsCreateFut
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
          override def outEdges: Seq[ICriteria] = {
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
        mNodes.multiGetRev(madIds2)
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
                    .++( Iterator(prodE, selfRcvrE) )
                }
              }
            ),
            ad = mad0.ad.copy(
              entities = {
                mad0.ad.entities.mapValues { ent =>
                  ent.copy(
                    text = ent.text.map { aosf =>
                      aosf.copy(
                        value = messages(aosf.value)
                      )
                    }
                  )
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

}


/** Интерфейс для DI-поля, содержащего инжектируемый экземпляр [[util.adn.NodesUtil]]. */
trait INodesUtil {
  def nodesUtil: NodesUtil
}
