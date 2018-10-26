package util.billing.cron

import javax.inject.Inject
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.model.n2.node.{MNodeTypes, MNodes}
import io.suggest.util.JMXBase
import io.suggest.util.logs.MacroLogsDyn
import models.mproj.ICommonDi
import play.api.inject.Injector
import util.adv.build.{AdvBuilderFactory, AdvBuilderUtil}
import util.adv.geo.tag.GeoTagsUtil

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.03.18 18:32
  * Description: Система повторной переактивации текущих размещений.
  */
class ReActivateCurrentAdvs @Inject() (
                                        override val advBuilderUtil     : AdvBuilderUtil,
                                        override val geoTagsUtil        : GeoTagsUtil,
                                        override val mNodes             : MNodes,
                                        override val mItems             : MItems,
                                        override val advBuilderFactory  : AdvBuilderFactory,
                                        override val mCommonDi          : ICommonDi
                                      )
  extends ActivateAdvs
{

  import mCommonDi._
  import slick.profile.api._

  override def MAX_ADS_PER_RUN = 30

  /** Тут требуется проходить все узлы, без каких-либо ограничений. */
  override final def MAX_ADS_PER_RUNS: Int = -1

  /** Это ребилд. Обязательно выставлять offset при поиске новых item'ов для обработки. */
  override final def isReBuild = true

  /** Ищем только карточки, у которых есть offline ads с dateStart < now. */
  override def _itemsSql(i: mItems.MItemsTable): Rep[Option[Boolean]] = {
    (i.statusStr === MItemStatuses.Online.value).?
  }


  /** Жесткий adv-ребилд вообще всех узлов, которые могут быть связаны.
    * @return Кол-во обработанных узлов.
    */
  def reBuildAllExistingNodes(): Future[Int] = {
    // Типы узлов, которые будут ребилдится:
    val msearch = new MNodeSearchDfltImpl {
      override val nodeTypes = MNodeTypes.AdnNode :: MNodeTypes.Ad :: Nil
    }
    lazy val logPrefix = s"reBuildAllExistingNodes()#${System.currentTimeMillis()}:"

    mNodes
      .source[String]( msearch.toEsQuery )
      .mapAsyncUnordered(4) { mnodeId =>
        LOGGER.trace(s"$logPrefix Processing node #$mnodeId...")
        runForNodeId(mnodeId)
          .recover { case ex: Throwable =>
            LOGGER.error(s"$logPrefix Error while processing node#$mnodeId", ex)
            None
          }
      }
      .runFold(0) { (acc, _) => acc + 1 }
  }

  override def hasItemsForProcessing(mitems: Iterable[MItem]): Boolean = {
    // Никогда не прерывать обработку узлов.
    true
  }

}


/** Интерфейс JMX MBean. */
trait ReActivateCurrentAdvsJmxMBean {

  /** Запустить пересборку всех online-размещений.
    * @return Отчёт по результатам.
    */
  def reActivateAllCurrentAdvs(): String

  /** Пересобрать размещения для указанной рекламной карточки.
    * @param nodeId id пересобираемой рекламной карточки.
    * @return Отчёт по результатам.
    */
  def reActivateCurrentAdvsForNode(nodeId: String): String

  /** Пересобрать узлы, затрагиваемые системой биллингового размещения.
    * @return Строка с отчётом по обработанным узлам.
    */
  def reBuildAllExistingNodes(): String

}

/** Реализация [[ReActivateCurrentAdvsJmxMBean]]. */
class ReActivateCurrentAdvsJmx @Inject() (
                                           injector: Injector,
                                           override implicit val ec: ExecutionContext
                                         )
  extends JMXBase
  with ReActivateCurrentAdvsJmxMBean
  with MacroLogsDyn
{

  override def jmxName = "io.suggest:type=util,name=" + getClass.getSimpleName.replace("Jmx", "")

  private def reActivateCurrentAdvs = injector.instanceOf[ReActivateCurrentAdvs]


  override def reActivateCurrentAdvsForNode(nodeId: String): String = {
    val fut = for {
      res <- reActivateCurrentAdvs.runForNodeId(nodeId)
    } yield {
      s"Done, result = $res"
    }
    awaitString(fut)
  }

  override def reActivateAllCurrentAdvs(): String = {
    val fut = for {
      res <- reActivateCurrentAdvs.run()
    } yield {
      s"Done, $res items processed."
    }
    awaitString(fut)
  }

  override def reBuildAllExistingNodes(): String = {
    val fut = for {
      res <- reActivateCurrentAdvs.reBuildAllExistingNodes()
    } yield {
      s"Done, $res nodes processed."
    }
    awaitString(fut)
  }

}
