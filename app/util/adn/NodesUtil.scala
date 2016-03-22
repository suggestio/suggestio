package util.adn

import com.google.inject.{Inject, Singleton}
import controllers.routes
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.edge.{MEdgeInfo, MNodeEdges}
import io.suggest.model.n2.extra.{MAdnExtra, MNodeExtras, MSlInfo}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.MBasicMeta
import io.suggest.model.n2.node.meta.colors.{MColorData, MColors}
import models._
import models.adv.MExtTarget
import models.madn.{MNodeRegSuccess, NodeDfltColors}
import models.mext.MExtServices
import models.mproj.ICommonDi
import org.joda.time.DateTime
import play.api.i18n.Messages
import play.api.mvc.Call
import util.PlayMacroLogsImpl

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
  mCommonDi               : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  /** Дефолтовый лимит на размещение у самого себя на главной. */
  private val SL_START_PAGE_LIMIT_DFLT: Int = configuration.getInt("user.node.adn.sl.out.statpage.limit.dflt") getOrElse 50

  /** Через сколько секунд отправлять юзера в ЛК ноды после завершения реги юзера. */
  private val NODE_CREATED_SUCCESS_RDR_AFTER: Int = configuration.getInt("user.node.created.success.redirect.after.sec") getOrElse 5

  // Для новосозданного узла надо создавать новые карточки, испортируя их из указанного узла в указанном кол-ве.
  /** id узла, который содержит дефолтовые карточки. Задается явно в конфиге. */
  val ADN_IDS_INIT_ADS_SOURCE = configuration.getStringSeq("user.node.created.mads.import.from.adn_ids") getOrElse Nil

  /** Кол-во карточек для импорта из дефолтового узла. */
  val INIT_ADS_COUNT = configuration.getInt("user.node.created.mads.import.count") getOrElse 1


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

  /**
   * Создать новый инстанс узла для юзера без сохранения узла в хранилище.
   * @param name название узла.
   * @param personId id юзера-владельца.
   * @return Экземпляр узла без id.
   */
  def userNodeInstance(name: String, personId: String): MNode = {
    MNode(
      common = MNodeCommon(
        ntype = MNodeTypes.AdnNode,
        isDependent = true,
        isEnabled = true
      ),
      meta = MMeta(
        basic = MBasicMeta(
          nameOpt = Some(name)
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
          rights          = Set(AdnRights.PRODUCER, AdnRights.RECEIVER),
          isUser          = true,
          shownTypeIdOpt  = Some(AdnShownTypes.SHOP.name),
          testNode        = false,
          showInScNl      = false,
          outSls          = {
            val sli = MSlInfo(AdShowLevels.LVL_START_PAGE, SL_START_PAGE_LIMIT_DFLT)
            Map(sli.sl -> sli)
          }
        ))
      ),
      edges = MNodeEdges(
        out = {
          val medge = MEdge(
            predicate = MPredicates.OwnedBy,
            nodeIdOpt = Some(personId)
          )
          MNodeEdges.edgesToMap(medge)
        }
      )
    )
  }

  /**
   * Создать дефолтовые таргеты для размещения в соц.сетях.
   * @param adnId id узла.
   * @return Фьючерс для синхронизации.
   */
  def createExtDfltTargets(adnId: String)(implicit lang: Messages): Future[_] = {
    val tgtsIter = MExtServices.values
      .iterator
      .flatMap { svc => MExtTarget.dfltTarget(svc, adnId) }
    Future.traverse(tgtsIter)(_.save)
  }

  /**
   * Создание нового узла для юзера. Узел должен быть готов к финансовой работе.
   * @param name Название узла.
   * @param personId id юзера-владельца.
   * @return Фьючерс с готовым инстансом нового существующего узла.
   */
  def createUserNode(name: String, personId: String)(implicit lang: Messages): Future[MNode] = {
    val inst = userNodeInstance(name = name, personId = personId)
    val nodeSaveFut = inst.save
    nodeSaveFut.flatMap { nodeId =>
      val madsCreateFut = installDfltMads(nodeId)
      for {
        _ <- createExtDfltTargets(nodeId)
        _ <- madsCreateFut
      } yield {
        inst.copy(id = Some(nodeId))
      }
    }
  }


  /** Установить дефолтовые карточки. */
  def installDfltMads(adnId: String, count: Int = INIT_ADS_COUNT)(implicit lang: Messages): Future[Seq[String]] = {
    lazy val logPrefix = s"installDfltMads($adnId):"
    (Future successful ADN_IDS_INIT_ADS_SOURCE)
      // Если нет продьюсеров, значит функция отключена. Это будет перехвачено в recover()
      .filter { _.nonEmpty }

      // Собрать id карточек, относящиеся к заданным узлам-источникам.
      .flatMap { prodIds =>
        val _limit = Math.max(50, count * 2)
        val dsa0 = new AdSearchImpl {
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
        MNode.dynSearchIds(dsa0)
      }

      // Случайно выбрать из списка id карточек только указанное кол-во карточек.
      .flatMap { madIds =>
        val count = madIds.size
        val rnd = new Random()
        val madIds2 = (0 until Math.min(count, count))
          .iterator
          .map { _ =>  madIds( rnd.nextInt(count) ) }
        MNode.multiGetRev(madIds2)
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
                dateCreated = DateTime.now,
                dateEdited = None
              )
            ),
            edges = MNodeEdges(
              out = {
                val pp = MPredicates.OwnedBy
                val rp = MPredicates.Receiver
                val someAdnId = Some(adnId)
                val prodE = MEdge(
                  predicate = pp,
                  nodeIdOpt = someAdnId
                )
                val selfRcvrE = MEdge(
                  predicate = rp,
                  nodeIdOpt = someAdnId,
                  info = MEdgeInfo(sls = Set(SinkShowLevels.GEO_START_PAGE_SL))
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
                        value = Messages(aosf.value)
                      )
                    }
                  )
                }
              }
            )
          )

          // Запустить сохранение сгенеренной карточки.
          mad1.save
        }
      }

      // Если не было adnId узлов-источников, то
      .recover { case ex: NoSuchElementException =>
        LOGGER.warn(logPrefix + " Node default ads installer is disabled!")
        Nil
      }
  }

}
