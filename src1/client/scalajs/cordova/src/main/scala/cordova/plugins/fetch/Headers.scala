package cordova.plugins.fetch

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.2020 20:38
  * Description: Интерфейс для инстансов Headers, собираемых на стороне fetch.js.
  */
@js.native
trait Headers extends js.Object {

  val map: js.Dictionary[js.Array[String]] = js.native

  def append(name: String, value: String): Unit = js.native
  def delete(name: String): Unit = js.native
  /** @return null | String */
  def get(name: String): String = js.native
  def getAll(name: String): js.Array[String] = js.native
  def has(name: String): Boolean = js.native
  def set(name: String, value: String): Unit = js.native
  /** @param f (value, name) => void. */
  def forEach(f: js.Function2[String, String, Unit]): Unit = js.native
}
