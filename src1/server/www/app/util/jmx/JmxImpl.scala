package util.jmx

import javax.inject.Inject
import io.suggest.model.n2.node.MNodesJmx
import models.adv.MExtTargetsJmx
import models.event.MEventsJmx
import models.mcal.MCalendarJmx
import java.lang.management.ManagementFactory

import io.suggest.loc.geo.ipgeobase.{IpgbImporterJmx, MCitiesJmx, MIpRangesJmx}
import io.suggest.sec.util.SCryptUtilJmx
import io.suggest.stat.inx.StatIndexUtilJmx
import io.suggest.stat.m.MStatsJmx
import io.suggest.util.JmxBase
import io.suggest.util.logs.MacroLogsImplLazy
import play.api.inject.ApplicationLifecycle
import util.adn.NodesUtilJmx
import util.adv.direct.AdvRcvrsUtilJmx
import util.adv.geo.AdvGeoRcvrsUtilJmx
import util.adv.geo.tag.GeoTagsUtilJmx
import util.billing.{Bill2UtilJmx, BillDebugUtilJmx, TfDailyUtilJmx}
import util.billing.cron.ReActivateCurrentAdvsJmx
import util.es.SiowebEsModelJmx
import util.img.DynImgUtilJmx

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 11:31
 * Description: JMX MBean'ы нужно заливать в энтерпрайз. Тут добавка к Global, чтобы тот мог включать/выключать jmx.
 */

case class JmxImpl @Inject()(
                               siowebEsModelJmx              : SiowebEsModelJmx,
                               advRcvrsUtilJmx               : AdvRcvrsUtilJmx,
                               mCalendarJmx                  : MCalendarJmx,
                               mNodesJmx                     : MNodesJmx,
                               geoTagsUtilJmx                : GeoTagsUtilJmx,
                               mExtTargetsJmx                : MExtTargetsJmx,
                               mEventsJmx                    : MEventsJmx,
                               mIpRangesJmx                  : MIpRangesJmx,
                               mCitiesJmx                    : MCitiesJmx,
                               mStatsJmx                     : MStatsJmx,
                               ipgbImporterJmx               : IpgbImporterJmx,
                               statIndexUtilJmx              : StatIndexUtilJmx,
                               sCryptUtilJmx                 : SCryptUtilJmx,
                               dynImgUtilJmx                 : DynImgUtilJmx,
                               reActivateCurrentAdvsJmx      : ReActivateCurrentAdvsJmx,
                               advGeoRcvrsUtilJmx            : AdvGeoRcvrsUtilJmx,
                               billDebugUtilJmx              : BillDebugUtilJmx,
                               bill2UtilJmx                  : Bill2UtilJmx,
                               tfDailyUtilJmx                : TfDailyUtilJmx,
                               nodesUtilJmx                  : NodesUtilJmx,
                               lifecycle                     : ApplicationLifecycle,
                               implicit private val ec       : ExecutionContext,
                             )
  extends MacroLogsImplLazy
{

  import LOGGER._

  /** Список моделей, отправляемых в MBeanServer. private для защиты от возможных воздействий извне. */
  private def JMX_MODELS = {
    productIterator
      .flatMap {
        case jmx: JmxBase => jmx :: Nil
        case _            => Nil
      }
      .toSeq
  }

  // Constructor
  registerAll()

  // Destructor
  lifecycle.addStopHook { () =>
    Future {
      unregisterAll()
    }
  }

  private def getSrv = ManagementFactory.getPlatformMBeanServer

  /** Глобально зарегать все поддерживаемые возможные MBean'ы. */
  def registerAll(): Unit = {
    val srv = getSrv
    for (jmxMB <- JMX_MODELS) {
      val name = jmxMB.jmxName
      try {
        srv.registerMBean(jmxMB, JmxBase.string2objectName(name) )
      } catch {
        case _: javax.management.InstanceAlreadyExistsException =>
          warn("Instance already registered: " + jmxMB)
        case ex: Exception =>
          error("Cannot register " + name, ex)
      }
    }
  }

  /** При выключении/перезапуске системы нужно провести де-регистрацию всех MBean'ов. */
  def unregisterAll(): Unit = {
    val srv = getSrv
    for (jmxMB <- JMX_MODELS) {
      val name = jmxMB.jmxName
      try {
        srv.unregisterMBean( JmxBase.string2objectName(name) )
      } catch {
        case _: javax.management.InstanceNotFoundException =>
          warn("JMX instance not registered: " + jmxMB)
        case ex: Exception =>
          warn("Cannot unregister " + name, ex)
      }
    }
  }

}
