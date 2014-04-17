package util.jmx

import io.suggest.ym.model._
import io.suggest.model._
import io.suggest.model.inx2._
import io.suggest.ym.model.stat._
import util.SiowebEsUtil.client
import util.event.SiowebNotifier.Implicts.sn
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import java.lang.management.ManagementFactory
import io.suggest.util.JMXBase

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 11:31
 * Description: JMX MBean'ы нужно заливать в энтерпрайз. Тут добавка к Global, чтобы тот мог включать/выключать jmx.
 */

object JMXImpl {
  import io.suggest.util.JMXHelpers._

  /** Список моделей, отправляемых в MBeanServer. private для защиты от возможных воздействий извне. */
  private val JMX_MODELS = List[JMXBase](
    new MMartInxJmx,
    new MAdStatJmx,
    new MWelcomeAdJmx,
    new MAdJmx,
    new MAdnNodeJmx,
    new MPictJmx
  )

  private def getSrv = ManagementFactory.getPlatformMBeanServer

  /** Глобально зарегать все поддерживаемые возможные MBean'ы. */
  def registerAll() {
    val srv = getSrv
    JMX_MODELS.foreach { jmxMB =>
      srv.registerMBean(jmxMB, jmxMB.jmxName)
    }
  }

  /** При выключении/перезапуске системы нужно провести де-регистрацию всех MBean'ов. */
  def unregisterAll() {
    val srv = getSrv
    JMX_MODELS.foreach { jmxMB =>
      srv.unregisterMBean(jmxMB.jmxName)
    }
  }

}

