package util.jmx

import com.google.inject.Inject
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.n2.media.MMediaJmx
import io.suggest.model.n2.node.MNodeJmx
import io.suggest.ym.model._
import io.suggest.model._
import io.suggest.ym.model.stat._
import models.adv.MExtTargetJmx
import models.ai.MAiMadJmx
import models.event.MEventJmx
import models.merr.MRemoteErrorJmx
import models.usr.{MExtIdentJmx, EmailActivationJmx, EmailPwIdentJmx}
import org.elasticsearch.client.Client
import util.adv.AdvUtilJmx
import java.lang.management.ManagementFactory
import io.suggest.util.JMXBase
import models._
import util.PlayLazyMacroLogsImpl
import io.suggest.util.JMXHelpers._
import util.compat._

import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 11:31
 * Description: JMX MBean'ы нужно заливать в энтерпрайз. Тут добавка к Global, чтобы тот мог включать/выключать jmx.
 */

class JMXImpl @Inject() (
  mMediaJmx                     : MMediaJmx,
  siowebEsModelJmx              : SiowebEsModelJmx,
  migration                     : img3.MigrationJmx,
  advUtilJmx                    : AdvUtilJmx,
  mCalendarJmx                  : MCalendarJmx,
  mInviteRequestJmx             : MInviteRequestJmx,
  mNodeJmx                      : MNodeJmx,
  implicit private val ec       : ExecutionContext,
  implicit private val esClient : Client,
  implicit private val sn       : SioNotifierStaticClientI
)
  extends PlayLazyMacroLogsImpl
{

  import LOGGER._

  /** Список моделей, отправляемых в MBeanServer. private для защиты от возможных воздействий извне. */
  private val JMX_MODELS = List[JMXBase](
    // compat
    // elasticsearch
    new MAdStatJmx,
    new MWelcomeAdJmx,
    new MAdJmx,
    new MAdnNodeJmx,
    new EmailActivationJmx,
    new EmailPwIdentJmx,
    new MExtIdentJmx,
    new MCompanyJmx,
    mCalendarJmx,
    mInviteRequestJmx,
    new MAdnNodeGeoJmx,
    siowebEsModelJmx,
    new MRemoteErrorJmx,
    new MAiMadJmx,
    advUtilJmx,
    new MEventJmx,
    new MExtTargetJmx,
    mNodeJmx,
    mMediaJmx,
    // cassandra
    new SioCassandraClientJmx,
    new MUserImgMeta2Jmx,
    new MUserImg2Jmx,
    // web21 compat
    migration
  )

  private def getSrv = ManagementFactory.getPlatformMBeanServer

  /** Глобально зарегать все поддерживаемые возможные MBean'ы. */
  def registerAll() {
    val srv = getSrv
    JMX_MODELS.foreach { jmxMB =>
      try {
        srv.registerMBean(jmxMB, jmxMB.jmxName)
      } catch {
        case _: javax.management.InstanceAlreadyExistsException =>
          warn("Instance already registered: " + jmxMB)
        case ex: Exception =>
          error("Cannot register " + jmxMB, ex)
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
        case _: javax.management.InstanceNotFoundException =>
          warn("JMX instance not registered: " + jmxMB)
        case ex: Exception =>
          warn("Cannot unregister " + jmxMB.jmxName, ex)
      }
    }
  }

}

