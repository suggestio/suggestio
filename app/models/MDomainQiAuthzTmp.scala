package models

import util._
import util.SiobixFs.fs
import org.apache.hadoop.fs.Path
import org.joda.time.{Duration, DateTime}
import io.suggest.model.JsonDfsBackend
import io.suggest.util.StorageType._
import com.fasterxml.jackson.annotation.JsonIgnore
import scala.concurrent.duration._
import util.DfsModelUtil._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.13 15:37
 * Description: временное промежуточное хранилище данных qi-проверки доменов для анонимусов.
 * Можно было бы использовать кеш, но это вызовет серьезные проблемы при масштабировании узлов с веб-мордами.
 */

final case class MDomainQiAuthzTmp(
  dkey : String,
  id: String,
  dateCreated: DateTime = DateTime.now()
) extends MDomainAuthzT with DkeyModelT {

  import MDomainQiAuthzTmp.{BACKEND, VERIFY_DURATION_HARD, VERIFY_DURATION_SOFT, LOGGER}
  import LOGGER._

  @JsonIgnore def personIdOpt: Option[String] = None
  @JsonIgnore def isValid: Boolean = {
    dateCreated.minus(VERIFY_DURATION_HARD).isAfterNow
  }
  @JsonIgnore def isNeedRevalidation: Boolean = {
    dateCreated.minus(VERIFY_DURATION_SOFT).isAfterNow
  }

  /**
   * Сохранить текущий ряд в базу.
   * @return
   */
  def save = {
    trace(s"save(): id=$id dkey=$dkey")
    BACKEND.save(this)
  }

  /**
   * Удалить файл, относящийся к текущему экземпляру класса.
   * @return true, если файл действительно удален.
   */
  def delete = {
    trace(s"delete(): id=$id dkey=$dkey")
    BACKEND.delete(dkey=dkey, id=id)
  }

  @JsonIgnore def isQiType: Boolean = true
  @JsonIgnore def isValidationType: Boolean = false

  def bodyCodeOpt: Option[String] = None
}


object MDomainQiAuthzTmp extends PlayMacroLogsImpl {

  private val BACKEND: Backend = {
    StorageUtil.STORAGE match {
      case DFS    => new DfsBackend
    }
  }

  val VERIFY_DURATION_SOFT = new Duration(45.minutes.toMillis)
  // Превышения хард-лимита означает, что верификация уже истекла и её нужно проверять заново.
  val VERIFY_DURATION_HARD = new Duration(60.minutes.toMillis)



  /**
   * Прочитать из временного хранилища ранее сохраненные данные по домену и qi.
   * @param dkey ключ домена
   * @param id qi id
   * @return Опциональный MDomainQiAuthzTmp
   */
  def getForDkeyId(dkey:String, id:String) = BACKEND.getForDkeyId(dkey, id)


  /** Интерфейс бэкэндов модели. */
  trait Backend {
    def save(data: MDomainQiAuthzTmp): Future[_]
    def delete(dkey:String, id:String): Future[Any]
    def getForDkeyId(dkey:String, id:String): Future[Option[MDomainQiAuthzTmp]]
    //def listDkey(dkey: String): Future[List[MDomainQiAuthzTmp]]
  }
  
  /** Бэкэнд для хранения данных модели в ФС. */
  class DfsBackend extends Backend {

    val tmpDirName = "qi_anon_tmp"
    val tmpDir = new Path(SiobixFs.siobix_conf_path, tmpDirName)

    def getDkeyDir(dkey:String): Path = {
      new Path(tmpDir, dkey)
    }

    def getFilePath(dkey:String, qi_id:String): Path = {
      new Path(tmpDir, dkey + "/" + qi_id)
    }

    def save(data: MDomainQiAuthzTmp): Future[_] = {
      Future {
        val filepath = getFilePath(dkey=data.dkey, qi_id=data.id)
        val os = fs.create(filepath)
        try {
          JsonDfsBackend.writeToPath(filepath, data)
        } finally {
          os.close()
        }
      }
    }

    def delete(dkey: String, id: String): Future[Any] = {
      Future {
        val filepath = getFilePath(dkey, id)
        val result = deleteNr(filepath)
        // Скорее всего, директория домена теперь пустая, и её тоже пора удалить для поддержания чистоты в tmp-директории модели.
        deleteNr(filepath.getParent)
        result
      }
    }

    def getForDkeyId(dkey: String, id: String): Future[Option[MDomainQiAuthzTmp]] = {
      Future {
        val filepath = getFilePath(dkey, id)
        readOne[MDomainQiAuthzTmp](filepath, fs)
      }
    }

  }

}