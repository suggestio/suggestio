package util.radius

import java.net.InetSocketAddress

import org.tinyradius.util.RadiusServer
import util.PlayMacroLogsImpl
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.09.14 13:32
 * Description: Реализация radius-сервера на базе tinyradius.
 */
object RadiusServerImpl extends RadiusServer with PlayMacroLogsImpl {

  import LOGGER._

  // На время раннего PoC-теста модели для хранения пока тут:
  private var smsPhoneCodes: Map[String, String] = Map.empty

  private def IS_ENABLED_CK = "radius.server.tiny.enabled"

  /** Узнать из конфига, есть ли включенный сервер? */
  def IS_ENABLED = configuration.getBoolean(IS_ENABLED_CK) getOrElse false

  def addPhone(phone: String, code: String) {
    smsPhoneCodes += (phone -> code)
  }

  override def getSharedSecret(client: InetSocketAddress): String = {
    "%shared_secret%"
  }

  override def getUserPassword(userName: String): String = {
    smsPhoneCodes.get(userName).orNull
  }

  override def start(listenAuth: Boolean, listenAcct: Boolean): Unit = {
    if (IS_ENABLED) {
      super.start(listenAuth, listenAcct)
      info("tinyradius server started.")
    } else {
      info(s"""tinyradius server not enabled in config. To enabled it add "$IS_ENABLED_CK = true" to application.conf.""")
    }
  }

  override def stop(): Unit = {
    if (IS_ENABLED) {
      super.stop()
      info("tinyradius server stopped.")
    } else {
      trace("tinyradius disabled in config. Nothing to stop().")
    }
  }

}
