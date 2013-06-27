package util

import gnu.inet.encoding.IDNA
import play.api.libs.json.{JsValue, JsString}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.06.13 18:53
 * Description: у многих объектов есть параметр dkey, содержащий нормализованный hostname. В ходе нормализации происходит
 * конверсия IDN -> ASCII, и юзерам тяжело понять что у них там за домен в списке.
 * Тут - примесь для таких объектов, чтобы у этих объектов была возможность нормального отображения dkey в виде Unicode-строки.
 */
trait DkeyContainerT {
  def dkey: String

  def dkeyUnicode: String = IDNA.toUnicode(dkey)
  def dkeyUnicodeJs = JsString(dkeyUnicode)
  def dkeyJsProps = List[(String, JsString)]("dkey" -> JsString(dkey), "domain" -> dkeyUnicodeJs)
}

object DkeyContainer {
  def dkeyJsProps(dkey: String): List[(String, JsString)] = List(
    "dkey"   -> JsString(dkey),
    "domain" -> JsString(IDNA.toUnicode(dkey))
  )
}