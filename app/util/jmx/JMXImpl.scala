package util.jmx

import io.suggest.ym.model._
import io.suggest.model._
import io.suggest.model.inx2._
import io.suggest.ym.model.stat._
import models.ai.MAiMadJmx
import models.event.MEventJmx
import util.SiowebEsUtil.client
import util.adv.AdvUtilJmx
import util.event.SiowebNotifier.Implicts.sn
import play.api.libs.concurrent.Execution.Implicits.defaultContext
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

object JMXImpl extends PlayLazyMacroLogsImpl {
  import LOGGER._

  /** Список моделей, отправляемых в MBeanServer. private для защиты от возможных воздействий извне. */
  private val JMX_MODELS = List[JMXBase](
    new MMartInxJmx,
    new MAdStatJmx,
    new MWelcomeAdJmx,
    new MAdJmx,
    new MAdnNodeJmx,
    new MMartCategoryJmx,
    new EmailActivationJmx,
    new EmailPwIdentJmx,
    new MPersonJmx,
    new MCompanyJmx,
    new MCalendarJmx,
    new MInviteRequestJmx,
    new MAdnNodeGeoJmx,
    new SiowebEsModelJmx,
    new MRemoteErrorJmx,
    new MAiMadJmx,
    new AdvUtilJmx,
    new MEventJmx,
    // cassandra
    new SioCassandraClientJmx,
    new MImgThumb2Jmx,
    new MUserImgMeta2Jmx,
    new MUserImg2Jmx
  )

  private def getSrv = ManagementFactory.getPlatformMBeanServer

  /** Глобально зарегать все поддерживаемые возможные MBean'ы. */
  def registerAll() {
    val srv = getSrv
    JMX_MODELS.foreach { jmxMB =>
      try {
        srv.registerMBean(jmxMB, jmxMB.jmxName)
      } catch {
        case ex: Exception => error("Cannot register " + jmxMB, ex)
      }
    }
  }

  /** При выключении/перезапуске системы нужно провести де-регистрацию всех MBean'ов. */
  def unregisterAll() {
    val srv = getSrv
    JMX_MODELS.foreach { jmxMB =>
      try {
        srv.unregisterMBean(jmxMB.jmxName)
      } catch {
        case ex: Exception => warn("Cannot unregister " + jmxMB.jmxName, ex)
      }
    }
  }

}

