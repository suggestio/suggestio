package models

import org.joda.time.DateTime
import io.suggest.model.JsonDfsBackend
import org.apache.hadoop.fs.Path
import util.SiobixFs
import SiobixFs.fs
import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.13 19:20
 * Description: Domain QuickInstall модель. Хранит промежуточные данные о домене и сессии юзера,
 * который использует быструю установку. Набор qi хранится в dout/$domain/conf/$id.json
 * Этого достаточно, т.к. обычно для домена есть только один qi и только непродолжительное время.
 */

case class MDomainQi(
  id           : String,
  dkey         : String,
  start_url    : String,
  session_id   : String,
  date_created : Long = DateTime.now.toInstant.getMillis
) {

  import MDomainQi._

  @JsonIgnore lazy val path = getPath(dkey, id)

  /**
   * Сохранить сие в хранилище
   */
  def save() {
    JsonDfsBackend.writeTo(path, this)
  }

  /**
   * Удалить этот qi из хранилища
   */
  def delete() = fs.delete(path, false)

}


// Статика модели
object MDomainQi {

  val DOMAIN_SUBDIR = "qi"

  /**
   * Адрес для папки в директории домена, которая хранит все данные qi
   * @param dkey ключ домена
   * @return
   */
  protected def getSubdirPath(dkey:String) = new Path(SiobixFs.dkeyPathConf(dkey), DOMAIN_SUBDIR)

  /**
   * Получить путь для сохранения указанной qi
   * @param dkey ключ домена
   * @param id идентификатор qi
   * @return
   */
  protected def getPath(dkey:String, id:String) : Path = {
    new Path(getSubdirPath(dkey), id)
  }


  /**
   * Прочитать из базы qi-шку
   * @param dkey ключ домена
   * @param id идентификатор qi
   * @return
   */
  def getForDkeyId(dkey:String, id:String) : Option[MDomainQi] = {
    val path = getPath(dkey, id)
    JsonDfsBackend.getAs[MDomainQi](path, fs)
  }

}
