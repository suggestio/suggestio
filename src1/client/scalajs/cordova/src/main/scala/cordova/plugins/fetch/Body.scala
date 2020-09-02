package cordova.plugins.fetch

import org.scalajs.dom.experimental.BodyInit
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.{Blob, FormData}

import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.2020 20:54
  * Description: API for Body() in fetch.js.
  */
@js.native
trait Body extends js.Object {

  var bodyUsed: Boolean = js.native

  val _bodyInit: js.UndefOr[BodyInit] = js.native
  val _bodyText: js.UndefOr[String] = js.native
  val _bodyBlob: js.UndefOr[Blob] = js.native
  val _bodyFormData: js.UndefOr[FormData] = js.native

  def _initBody(body: Ajax.InputData): Unit = js.native

  def blob(): js.Promise[Blob] = js.native
  def arrayBuffer(): js.Promise[ArrayBuffer] = js.native
  def text(): js.Promise[String] = js.native
  def formData(): js.Promise[FormData] = js.native
  def json(): js.Promise[js.Dynamic] = js.native

}
