package models

import util.{DkeyModelT, Logs, SiobixFs}
import util.SiobixFs.fs
import org.apache.hadoop.fs.Path
import org.joda.time.{Duration, DateTime}
import io.suggest.model.JsonDfsBackend
import com.fasterxml.jackson.annotation.JsonIgnore
import scala.concurrent.duration._
import util.DfsModelUtil._

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
) extends MDomainAuthzT with DkeyModelT {

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
  def delete = {
    val result = deleteNr(filepath)
    // Скорее всего, директория домена теперь пустая, и её тоже пора удалить для поддержания чистоты в tmp-директории модели.
    deleteNr(filepath.getParent)
    result
  }

  @JsonIgnore def isQiType: Boolean = true
  @JsonIgnore def isValidationType: Boolean = false


  override def qiTmpAuthPerson(qi_id: String): Option[MDomainQiAuthzTmp] = {
    if(qi_id == id)
      Some(this)
    else
      super.qiTmpAuthPerson(qi_id)
  }
}


object MDomainQiAuthzTmp extends Logs {

  val tmpDirName = "qi_anon_tmp"
  val tmpDir = new Path(SiobixFs.siobix_conf_path, tmpDirName)

  val VERIFY_DURATION_SOFT = new Duration(45.minutes.toMillis)
  // Превышения хард-лимита означает, что верификация уже истекла и её нужно проверять заново.
  val VERIFY_DURATION_HARD = new Duration(60.minutes.toMillis)

  def getDkeyDir(dkey:String): Path = {
    new Path(tmpDir, dkey)
  }

  def getFilePath(dkey:String, qi_id:String): Path = {
    new Path(tmpDir, dkey + "/" + qi_id)
  }


  /**
   * Прочитать из временного хранилища ранее сохраненные данные по домену и qi.
   * @param dkey ключ домена
   * @param id qi id
   * @return Опциональный MDomainQiAuthzTmp
   */
  def get(dkey:String, id:String): Option[MDomainQiAuthzTmp] = {
    val filepath = getFilePath(dkey, id)
    readOne[MDomainQiAuthzTmp](filepath, fs)
  }


  /**
   * Выдать список временных авторизация для указанного домена.
   * @param dkey Ключ домена.
   * @return Список сабжей в неопределенном порядке.
   */
  def listDkey(dkey:String): List[MDomainQiAuthzTmp] = {
    val path = getDkeyDir(dkey)
    fs.listStatus(path)
      .toList
      .foldLeft(List[MDomainQiAuthzTmp]()) { (acc, s) =>
        readOneAcc[MDomainQiAuthzTmp](acc, s.getPath, fs)
      }
  }

}