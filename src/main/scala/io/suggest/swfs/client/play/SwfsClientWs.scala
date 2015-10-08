package io.suggest.swfs.client.play

import com.google.inject.{Singleton, Inject}
import io.suggest.swfs.client.ISwfsClient
import io.suggest.util.{MacroLogsI, MacroLogsImpl}
import play.api.Configuration
import play.api.libs.ws.WSClient

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.10.15 19:53
 * Description: Seaweedfs client для построения swfs-моделей на его базе.
 * Модели живут на стороне play, поэтому в качестве клиента жестко привязан WSClient c DI.
 */

/** Интерфейс play.ws-клиента для написания трейтов частичных реализаций. */
trait ISwfsClientWs extends ISwfsClient with MacroLogsI {

  /** play.ws-клиента (http-клиент).  */
  def ws    : WSClient

  /** Конфиг play application. */
  def conf  : Configuration

}


/** DI-реализация высокоуровневого seaweedfs-клиента. */
@Singleton
class SwfsClientWs @Inject() (
  override val ws    : WSClient,
  override val conf  : Configuration
)
  extends MacroLogsImpl
  with Assign
