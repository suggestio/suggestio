package models

import util.DfsModelStaticT
import util.SiobixFs
import util.SiobixFs.fs
import org.apache.hadoop.fs.Path
import org.joda.time.DateTime
import io.suggest.model.JsonDfsBackend

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
  qi_id: String,
  date_created: DateTime = DateTime.now()
) {

  import MDomainQiAuthzTmp.getFilePath

  lazy val filepath = getFilePath(dkey, qi_id)

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

}


object MDomainQiAuthzTmp {

  val tmpDirName = "qi_anon_tmp"
  val tmpDir = new Path(SiobixFs.siobix_conf_path, tmpDirName)

  def getFilePath(dkey:String, qi_id:String): Path = {
    new Path(tmpDir, dkey + "~" + qi_id)
  }


  /**
   * Прочитать из временного хранилища ранее сохраненные данные по домену и qi.
   * @param dkey ключ домена
   * @param qi_id qi id
   * @return Опциональный MDomainQiAuthzTmp
   */
  def get(dkey:String, qi_id:String): Option[MDomainQiAuthzTmp] = {
    val filepath = getFilePath(dkey, qi_id)
    JsonDfsBackend.getAs[MDomainQiAuthzTmp](filepath, fs)
  }

}