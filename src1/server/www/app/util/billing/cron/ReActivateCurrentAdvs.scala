package util.billing.cron

import javax.inject.Inject

import io.suggest.mbill2.m.item.MItems
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.model.n2.node.MNodes
import io.suggest.util.JMXBase
import io.suggest.util.logs.MacroLogsDyn
import models.mproj.ICommonDi
import play.api.inject.Injector
import util.adv.build.{AdvBuilderFactory, AdvBuilderUtil}
import util.adv.geo.tag.GeoTagsUtil

import scala.concurrent.ExecutionContext

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

  /** Ищем только карточки, у которых есть offline ads с dateStart < now. */
  override def _itemsSql(i: mItems.MItemsTable): Rep[Option[Boolean]] = {
    (i.statusStr === MItemStatuses.Online.value).?
  }

}


/** Интерфейс JMX MBean. */
trait ReActivateCurrentAdvsJmxMBean {

  /** Запустить пересборку всех online-размещений.
    * @return Отчёт по результатам.
    */
  def reActivateAllCurrentAdvs(): String

  /** Пересобрать размещения для указанной рекламной карточки.
    * @param adId id пересобираемой рекламной карточки.
    * @return Отчёт по результатам.
    */
  def reActivateCurrentAdvsForNode(adId: String): String

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


  override def reActivateCurrentAdvsForNode(adId: String): String = {
    val fut = for {
      res <- reActivateCurrentAdvs.runForNodeId(adId)
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

}
