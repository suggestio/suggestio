package util.jmx

import io.suggest.es.model.EsModelJmx

import javax.inject.Inject
import io.suggest.n2.node.{MNodesJmx, SioMainEsIndexJmx}
import models.adv.MExtTargetsJmx

import java.lang.management.ManagementFactory
import io.suggest.geo.ipgeobase.{IpgbImporterJmx, MIpgbItemsJmx}
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
import util.img.{DynImgUtilJmx, ImgMaintainUtilJmx}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 11:31
 * Description: JMX MBean'ы нужно заливать в энтерпрайз. Тут добавка к Global, чтобы тот мог включать/выключать jmx.
 */

case class JmxImpl @Inject() (
                               esModelJmx                    : EsModelJmx,
                               siowebEsModelJmx              : SiowebEsModelJmx,
                               advRcvrsUtilJmx               : AdvRcvrsUtilJmx,
                               mNodesJmx                     : MNodesJmx,
                               geoTagsUtilJmx                : GeoTagsUtilJmx,
                               mExtTargetsJmx                : MExtTargetsJmx,
                               MIpgbItemsJmx                 : MIpgbItemsJmx,
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
                               imgMaintainUtilJmx            : ImgMaintainUtilJmx,
                               lifecycle                     : ApplicationLifecycle,
                               sioMainEsIndexJmx             : SioMainEsIndexJmx,
                               implicit private val ec       : ExecutionContext,
                             )
  extends MacroLogsImplLazy
{

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
          LOGGER.warn(s"Instance already registered: $jmxMB")
        case ex: Exception =>
          LOGGER.error(s"Cannot register $name", ex)
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
          LOGGER.warn(s"JMX instance not registered: $jmxMB")
        case ex: Exception =>
          LOGGER.warn(s"Cannot unregister $name", ex)
      }
    }
  }

}
