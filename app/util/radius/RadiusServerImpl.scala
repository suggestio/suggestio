package util.radius

import java.net.InetSocketAddress

import org.tinyradius.util.RadiusServer
import util.PlayMacroLogsImpl

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

  def addPhone(phone: String, code: String) {
    smsPhoneCodes += (phone -> code)
  }

  override def getSharedSecret(client: InetSocketAddress): String = {
    "%shared_secret%"
  }

  override def getUserPassword(userName: String): String = {
    smsPhoneCodes.get(userName).orNull
  }

}
