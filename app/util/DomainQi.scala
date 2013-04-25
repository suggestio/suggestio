package util

import play.api.Play.current
import play.api.mvc.Session
import play.api.libs.concurrent.Akka

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.13 16:45
 * Description: Domain Quick Install - система быстрого подключения доменов к suggest.io. Юзер добавляет домен,
 * ему выдается js-код с ключиком, этот же ключик прописывается в сессии. Затем, при запросе скрипта от имени этого
 * домена происходит проверка наличия на сайте нужного скрипта.
 */

object DomainQi {

  /**
   * Прочитать из сессии список быстро добавленных в систему доменов и прилинковать их к текущему юзеру.
   * Домен появляется в сессии юзера только, когда qi-проверялка реквестует страницу со скриптом и проверит все данные.
   * @param email email юзера, т.е. его id
   * @param session Неизменяемые данные сессии. Все данные сессии будут безвозвратно утрачены после завершения этого метода.
   */
  def installFromSession(email:String, session:Session) {
    println("installFromSession(): Not yet implemented")
  }

}
