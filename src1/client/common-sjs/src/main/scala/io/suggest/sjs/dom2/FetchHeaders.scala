package io.suggest.sjs.dom2

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.2020 15:21
  */
@js.native
trait FetchHeaders extends js.Object {

  /** f: value, name => * */
  def forEach(f: js.Function2[String, String, Unit]): Unit = js.native

  @JSName("getAll")
  val getAllUnd: js.UndefOr[js.Function1[String, js.Array[String]]] = js.native
  def getAll(name: String): js.Array[String] = js.native

  /** Прямой доступ к private-полю внутри Headers() в API cordova-plugin-fetch.
    * Поле `map` в рамках fetch-спеки является аргументом конструктора Headers, и может быть (в теории) доступно. */
  @JSName("map")
  val _map: js.UndefOr[js.Dictionary[js.Array[String]]] = js.native

}
