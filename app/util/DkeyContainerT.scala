package util

import gnu.inet.encoding.IDNA
import play.api.libs.json.JsString
import models._
import scala.concurrent.ExecutionContext

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

  /**
   * Сгенерить поля для JSON со значениями dkey и domain (utf).
   * @param dkey исходный ключ домена (нормализованный)
   * @return Список-хвост для json-списка.
   */
  def dkeyJsProps(dkey: String): List[(String, JsString)] = List(
    "dkey"   -> JsString(dkey),
    "domain" -> JsString(IDNA.toUnicode(dkey))
  )
}

trait DkeyModelT extends DkeyContainerT {
  def domainOpt = MDomain.getForDkey(dkey)
  def domain    = domainOpt.get
  def domainUserSettings = MDomainUserSettings.getForDkey(dkey)
  def domainUserSettingsAsync = MDomainUserSettings.getForDkeyAsync(dkey)
  def authzForPerson(person_id:String) = MPersonDomainAuthz.getForPersonDkey(dkey, person_id)
  //def qiTmpAuth = MDomainQiAuthzTmp.listDkey(dkey)  // unused
  def qiTmpAuthPerson(qi_id: String) = MDomainQiAuthzTmp.getForDkeyId(dkey=dkey, id=qi_id)
  def domainSettingsFut(implicit ec:ExecutionContext) = MDomainSettings.getForDkey(dkey)
  def domainUserJson = MDomainUserJson.getForDkey(dkey)
}