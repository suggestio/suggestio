package io.suggest.swfs.client.play

import com.google.inject.{ImplementedBy, Singleton, Inject}
import io.suggest.swfs.client.ISwfsClient
import io.suggest.util.{MacroLogsI, MacroLogsImpl}
import play.api.Configuration
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.10.15 19:53
 * Description: Seaweedfs client для построения swfs-моделей на его базе.
 * Модели живут на стороне play, поэтому в качестве клиента жестко привязан WSClient c DI.
 */

object SwfsClientWs {

  def isStatus2xx(status: Int): Boolean = {
    status >= 200 && status <= 299
  }
  /** Название conf-ключа со списком доступных мастер-серверов seaweedfs. */
  def MASTERS_CK = "swfs.masters"

}


import SwfsClientWs._


/** Интерфейс play.ws-клиента для написания трейтов частичных реализаций. */
@ImplementedBy( classOf[SwfsClientWs] )
trait ISwfsClientWs extends ISwfsClient with MacroLogsI {

  implicit protected def ws: WSClient

  /** Конфиг play application. */
  protected def conf: Configuration

  def MASTERS: List[String]

  def MASTER_PROTO = "http"

}


/** DI-реализация высокоуровневого seaweedfs-клиента. */
@Singleton
class SwfsClientWs @Inject() (
  override val conf         : Configuration,
  override implicit val ws  : WSClient,
  override implicit val ec  : ExecutionContext
)
  extends MacroLogsImpl
  with Assign
  with Put
  with Get
  with Delete
  with Lookup
  with IsExist
{

  /**
   * Список weed-мастеров.
   *
   * @return ["localhost:9333", "127.5.5.5:9334"]
   */
  val MASTERS: List[String] = {
    conf.getStringSeq(MASTERS_CK)
      .filter(_.nonEmpty)
      .map { _.toList }
      .getOrElse {
        val dflt = "localhost:9333"
        LOGGER.warn("SeaweedFS masters are undefined/empty. Please define in config:\n  " +
          MASTERS_CK + " = [\"" + dflt + "\"]" )
        List(dflt)
      }
  }

}
