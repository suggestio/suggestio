package util

import play.api.mvc.Session
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.13 16:45
 * Description: Domain Quick Install - система быстрого подключения доменов к suggest.io. Юзер добавляет домен,
 * ему выдается js-код с ключиком, этот же ключик прописывается в сессии. Затем, при запросе скрипта от имени этого
 * домена происходит проверка наличия на сайте нужного скрипта.
 *
 * Суть этого модуля - дергать DomainRequester и прикручивать асинхронный анализатор результата к фьючерсу.
 * Анализатор должен парсить htmk и искать на странице тег скрипта suggest.io и определять из него домен и qi_id,
 * затем проверять по базе и порождать какое-то событие или предпринимать иные действия.
 */


// Статический клиент к DomainRequester, который выполняет запросы к доменам
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


  /**
   * Быстро создать фьючерс и скомбинировать его с парсером html и анализатором всея добра.
   * @param dkey ключ домена
   * @param url ссылка. Обычно на гланге
   * @param qi_id заявленный юзером qi_id, если есть. Может и не быть, если в момент установки на сайт зашел кто-то
   *              другой без qi_id в сессии.
   */
  def asyncCheckQi(dkey:String, url:String, qi_id:Option[String]) {
    DomainRequester.queueUrl(dkey, url).foreach { _.foreach { case DRResp(ct, istream) =>
      // Есть сырой ответ сайта. Нужно запустить тику с единственным SAX-handler и понять, если там тег скрипта suggest.io
      ???
    }
    }
  }

}

