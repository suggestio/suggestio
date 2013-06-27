package models

import util.SiobixFs
import util.SiobixFs.fs
import org.apache.hadoop.fs.Path
import org.joda.time.{Duration, DateTime}
import io.suggest.model.JsonDfsBackend
import com.fasterxml.jackson.annotation.JsonIgnore
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.13 15:37
 * Description: временное промежуточное хранилище данных qi-проверки доменов для анонимусов.
 * В качестве backend-хранилища отдельная папка в JSON DFS.
 * Можно было бы использовать кеш, но это вызовет серьезные проблемы при масштабировании узлов с веб-мордами.
 */

case class MDomainQiAuthzTmp(
  dkey : String,
  id: String,
  date_created: DateTime = DateTime.now()
) extends MDomainAuthzT {

  import MDomainQiAuthzTmp.{getFilePath, VERIFY_DURATION_HARD, VERIFY_DURATION_SOFT}

  @JsonIgnore def personIdOpt: Option[String] = None
  @JsonIgnore def isValid: Boolean = {
    date_created.minus(VERIFY_DURATION_HARD).isAfterNow
  }
  @JsonIgnore def isNeedRevalidation: Boolean = {
    date_created.minus(VERIFY_DURATION_SOFT).isAfterNow
  }

  @JsonIgnore lazy val filepath = getFilePath(dkey, id)

  /**
   * Сохранить текущий ряд в базу.
   * @return
   */
  def save: MDomainQiAuthzTmp = {
    val os = fs.create(filepath)
    try {
      JsonDfsBackend.writeTo(filepath, this)
      this

    } finally {
      os.close()
    }
  }

  /**
   * Удалить файл, относящийся к текущему экземпляру класса.
   * @return true, если файл действительно удален.
   */
  def delete = fs.delete(filepath, false)

  @JsonIgnore def isQiType: Boolean = true
  @JsonIgnore def isValidationType: Boolean = false
}


object MDomainQiAuthzTmp {

  val tmpDirName = "qi_anon_tmp"
  val tmpDir = new Path(SiobixFs.siobix_conf_path, tmpDirName)

  val VERIFY_DURATION_SOFT = new Duration(45.minutes.toMillis)
  // Превышения хард-лимита означает, что верификация уже истекла и её нужно проверять заново.
  val VERIFY_DURATION_HARD = new Duration(60.minutes.toMillis)


  def getFilePath(dkey:String, qi_id:String): Path = {
    new Path(tmpDir, dkey + "~" + qi_id)
  }


  /**
   * Прочитать из временного хранилища ранее сохраненные данные по домену и qi.
   * @param dkey ключ домена
   * @param id qi id
   * @return Опциональный MDomainQiAuthzTmp
   */
  def get(dkey:String, id:String): Option[MDomainQiAuthzTmp] = {
    val filepath = getFilePath(dkey, id)
    JsonDfsBackend.getAs[MDomainQiAuthzTmp](filepath, fs)
  }

}