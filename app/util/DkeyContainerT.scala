package util

import gnu.inet.encoding.IDNA
import play.api.libs.json.JsString
import scala.concurrent.{Future, ExecutionContext}
import com.fasterxml.jackson.annotation.JsonIgnore
import models._

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

  @JsonIgnore def dkeyUnicode: String = IDNA.toUnicode(dkey)
  @JsonIgnore def dkeyUnicodeJs = JsString(dkeyUnicode)
  @JsonIgnore def dkeyJsProps = List[(String, JsString)]("dkey" -> JsString(dkey), "domain" -> dkeyUnicodeJs)
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
  @JsonIgnore
  def domainOpt = MDomain.getForDkey(dkey)

  @JsonIgnore
  def domainUserSettings = MDomainUserSettings.getForDkey(dkey)

  def authzForPerson(person_id:String) = MPersonDomainAuthz.getForPersonDkey(dkey, person_id)
  //def qiTmpAuth = MDomainQiAuthzTmp.listDkey(dkey)  // unused
  def qiTmpAuthPerson(qi_id: String) = MDomainQiAuthzTmp.getForDkeyId(dkey=dkey, id=qi_id)

  @JsonIgnore
  def allPersonAuthz = MPersonDomainAuthz.getForDkey(dkey)

  @JsonIgnore
  def domainUserJson: Future[Option[MDomainUserJson]] = MDomainUserJson.getForDkey(dkey)
}