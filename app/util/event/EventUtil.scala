package util.event

import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.JsValue
import util.Acl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.06.13 10:36
 * Description: Набор утили для событий: например, подписка на всякие события.
 */

object EventUtil {

  type EventIO_t = (Iteratee[JsValue, Unit], Enumerator[JsValue])

  // Статическая раздача пустых каналов ввода-вывода.
  val dummyIn  = Iteratee.ignore[JsValue]
  val dummyOut = Enumerator[JsValue]()
  val dummyIO : EventIO_t = (dummyIn, dummyOut)

  /**
   * Раздача базовых каналов для вебсокетов юзера.
   * @param pw_opt acl-данные о юзере, обычно приходят неявно из экшенов.
   * @return In и Out каналы, пригодные для раздачи по websocket и комбинированию в контроллерах под конкретные задачи.
   */
  def globalUserEventIO(implicit pw_opt:Acl.PwOptT) : EventIO_t = {
    // TODO нужно для владельцев сайтов подцеплять события и важные уведомления какие-то для обратной связи.
    dummyIO
  }

}
