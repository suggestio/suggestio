package io.suggest.xadv.ext.js.fb.c.low

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.15 17:50
 * Description: Описание глобального интерфейса
 */

@JSName("FB")
object FbLow extends js.Object {

  /**
   * Инициализация клиента API.
   * @param options Настройки, такие как app_id.
   */
  def init(options: js.Dictionary[js.Any]): Unit = js.native

  /**
   * Запуск процедуры логина.
   * @param callback Функция реакции на результат логина.
   */
  def login(callback: js.Function1[js.Dictionary[js.Any], _],
            args: js.Dictionary[js.Any] ): Unit = js.native

  /**
   * Запустить асинхронное получение
   * @param callback function(response)
   * @param force Force reloading the login status (default false).
   */
  def getLoginStatus(callback: js.Function1[js.Dictionary[js.Any], _],
                     force: Boolean = js.native): Unit = js.native

  /**
   * Синхронно получить текущий (последний) экземпляр объекта authResponse,
   * полученные ранее через login/getLoginStatus.
   * @see [[https://developers.facebook.com/docs/reference/javascript/FB.getAuthResponse]]
   * @see [[https://developers.facebook.com/docs/reference/javascript/FB.getLoginStatus#response_and_session_objects]]
   * @return Экземпляр JSON authResponse или null.
   */
  def getAuthResponse(): js.Dictionary[js.Any] = js.native

  /**
   * Вызов к Facebook HTTP API.
   * @param path HTTP-путь для запроса.
   * @param httpMethod HTTP-метод для запроса.
   * @param args JSON-параметры вызова.
   * @param callback Реакция на результат вызова.
   */
  def api(path        : String,
          httpMethod  : String,
          args        : js.Dictionary[js.Any],
          callback    : js.Function1[js.Dictionary[js.Any], _] ): Unit = js.native

}
