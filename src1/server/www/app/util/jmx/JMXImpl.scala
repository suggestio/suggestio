package util.jmx

import com.google.inject.Inject
import io.suggest.model.n2.media.MMediasJmx
import io.suggest.model.n2.node.MNodesJmx
import models.adv.MExtTargetsJmx
import models.ai.MAiMadJmx
import models.event.MEventsJmx
import models.mcal.MCalendarJmx
import models.usr.{EmailActivationsJmx, EmailPwIdentsJmx, MExtIdentJmx}
import java.lang.management.ManagementFactory

import io.suggest.loc.geo.ipgeobase.{MCitiesJmx, MIpRangesJmx}
import io.suggest.stat.m.MStatsJmx
import io.suggest.util.JMXBase
import util.PlayLazyMacroLogsImpl
import io.suggest.util.JMXHelpers._
import play.api.inject.ApplicationLifecycle
import util.adv.direct.AdvRcvrsUtilJmx
import util.adv.geo.tag.GeoTagsUtilJmx
import util.es.SiowebEsModelJmx

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 11:31
 * Description: JMX MBean'ы нужно заливать в энтерпрайз. Тут добавка к Global, чтобы тот мог включать/выключать jmx.
 */

class JMXImpl @Inject() (
                          mMediasJmx                    : MMediasJmx,
                          siowebEsModelJmx              : SiowebEsModelJmx,
                          advUtilJmx                    : AdvRcvrsUtilJmx,
                          mCalendarJmx                  : MCalendarJmx,
                          mNodesJmx                     : MNodesJmx,
                          geoTagsUtilJmx                : GeoTagsUtilJmx,
                          mExtTargetsJmx                : MExtTargetsJmx,
                          mEventsJmx                    : MEventsJmx,
                          mAiMadJmx                     : MAiMadJmx,
                          emailPwIdentsJmx              : EmailPwIdentsJmx,
                          emailActivationsJmx           : EmailActivationsJmx,
                          mExtIdentJmx                  : MExtIdentJmx,
                          mIpRangesJmx                  : MIpRangesJmx,
                          mCitiesJmx                    : MCitiesJmx,
                          mStatsJmx                     : MStatsJmx,
                          lifecycle                     : ApplicationLifecycle,
                          implicit private val ec       : ExecutionContext
)
  extends PlayLazyMacroLogsImpl
{

  import LOGGER._

  /** Список моделей, отправляемых в MBeanServer. private для защиты от возможных воздействий извне. */
  private def JMX_MODELS = {
    List[JMXBase](
      emailActivationsJmx,
      emailPwIdentsJmx,
      mExtIdentJmx,
      mCalendarJmx,
      siowebEsModelJmx,
      mAiMadJmx,
      advUtilJmx,
      mEventsJmx,
      mExtTargetsJmx,
      mNodesJmx,
      geoTagsUtilJmx,
      mIpRangesJmx,
      mCitiesJmx,
      mMediasJmx,
      mStatsJmx
    )
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
        srv.registerMBean(jmxMB, name)
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
        srv.unregisterMBean(name)
      } catch {
        case _: javax.management.InstanceNotFoundException =>
          warn("JMX instance not registered: " + jmxMB)
        case ex: Exception =>
          warn("Cannot unregister " + name, ex)
      }
    }
  }

}
