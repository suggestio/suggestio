package util.billing.cron

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.n2.node.MNodeTypes
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.util.JmxBase
import play.api.inject.Injector

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.03.18 18:32
  * Description: Система повторной переактивации текущих размещений.
  */
final class ReActivateCurrentAdvs @Inject() (
                                              override val injector: Injector,
                                            )
  extends ActivateAdvs
{

  import esModel.api._
  import slickHolder.slick
  import slick.profile.api._


  /** Ищем только карточки, у которых есть offline ads с dateStart < now. */
  override def _itemsSql(i: mItems.MItemsTable): Rep[Option[Boolean]] = {
    (i.statusStr === MItemStatuses.Online.value).?
  }


  /** Жесткий adv-ребилд вообще всех узлов, которые могут быть связаны.
    * @return Кол-во обработанных узлов.
    */
  def reBuildAllExistingNodes(): Future[Int] = {
    // Типы узлов, которые будут ребилдится:
    val msearch = new MNodeSearch {
      override val nodeTypes = MNodeTypes.AdnNode :: MNodeTypes.Ad :: Nil
    }
    lazy val logPrefix = s"reBuildAllExistingNodes()#${System.currentTimeMillis()}:"

    mNodes
      .source[String]( msearch.toEsQuery )
      // Параллелизм выключен, чтобы завершить отладку и пересборку тегов.
      .mapAsyncUnordered(1) { mnodeId =>
        LOGGER.trace(s"$logPrefix Processing node #$mnodeId...")
        runForNodeId(mnodeId)
          .recover { case ex: Throwable =>
            LOGGER.error(s"$logPrefix Error while processing node#$mnodeId", ex)
            None
          }
      }
      .runWith( streamsUtil.Sinks.count )
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
                                           injector                   : Injector,
                                           implicit private val ec    : ExecutionContext,
                                         )
  extends JmxBase
  with ReActivateCurrentAdvsJmxMBean
{
  import io.suggest.util.JmxBase._

  override def _jmxType = Types.UTIL

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
