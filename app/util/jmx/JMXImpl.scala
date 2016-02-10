package util.jmx

import com.google.inject.Inject
import io.suggest.model.n2.media.MMediaJmx
import io.suggest.model.n2.node.MNodeJmx
import io.suggest.ym.model._
import io.suggest.ym.model.stat._
import models.adv.MExtTargetJmx
import models.ai.MAiMadJmx
import models.event.MEventJmx
import models.mcal.MCalendarJmx
import models.merr.MRemoteErrorJmx
import models.mproj.ICommonDi
import models.usr.{MExtIdentJmx, EmailActivationJmx, EmailPwIdentJmx}
import util.adv.AdvUtilJmx
import java.lang.management.ManagementFactory
import io.suggest.util.JMXBase
import models._
import util.PlayLazyMacroLogsImpl
import io.suggest.util.JMXHelpers._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 11:31
 * Description: JMX MBean'ы нужно заливать в энтерпрайз. Тут добавка к Global, чтобы тот мог включать/выключать jmx.
 */

class JMXImpl @Inject() (
  mMediaJmx                     : MMediaJmx,
  siowebEsModelJmx              : SiowebEsModelJmx,
  advUtilJmx                    : AdvUtilJmx,
  mCalendarJmx                  : MCalendarJmx,
  mInviteRequestJmx             : MInviteRequestJmx,
  mNodeJmx                      : MNodeJmx,
  mCommonDi                     : ICommonDi
)
  extends PlayLazyMacroLogsImpl
{

  import LOGGER._

  /** Список моделей, отправляемых в MBeanServer. private для защиты от возможных воздействий извне. */
  private val JMX_MODELS = {
    import mCommonDi._
    List[JMXBase](
      // compat
      // elasticsearch
      new MAdStatJmx,
      new EmailActivationJmx,
      new EmailPwIdentJmx,
      new MExtIdentJmx,
      new MCompanyJmx,
      mCalendarJmx,
      mInviteRequestJmx,
      siowebEsModelJmx,
      new MRemoteErrorJmx,
      new MAiMadJmx,
      advUtilJmx,
      new MEventJmx,
      new MExtTargetJmx,
      mNodeJmx,
      mMediaJmx
    )
  }

  private def getSrv = ManagementFactory.getPlatformMBeanServer

  /** Глобально зарегать все поддерживаемые возможные MBean'ы. */
  def registerAll() {
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
  def unregisterAll() {
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

