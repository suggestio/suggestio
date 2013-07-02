package io.suggest.index_info

import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.util.SiobixFs.fs
import io.suggest.model.MIndexInfo._
import java.io.{InputStreamReader, BufferedReader}
import io.suggest.util.Logs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.13 17:26
 * Description: SEARCH-файл содержит указатель на Active-индекс, который будет по-дефолту использоваться для поиска в рамках dkey.
 */

object MiiSearchPtr extends Logs {

  val prefix    = ""
  val name      = "SEARCH"
  val readBufSz = 32

  /**
   * Узнать идентификатор Current-файла, который содержит информацию по индексу, в котором следует проводить поиск.
   * @param dkey ключ домена.
   * @return Опциональную строку, которую с префиксом @ можно считать за имя файла, который содержит данные об индексе.
   */
  def getForDkey(dkey:String) : Option[String] = {
    val path = getFilePath(dkey, name)
    try {
      val is = fs.open(path, readBufSz)
      try {
        val isr = new InputStreamReader(is)
        val str = new BufferedReader(isr).readLine()
        Some(str)

      } finally {
        is.close()
      }

    } catch {
      case ex:Throwable =>
        error("Cannot read SEARCH file %s" format path, ex)
        None
    }
  }

}


/**
 * SEARCH-файл для сохранения указателя на дефолтовый поисковый индекс.
 * @param dkey ключ домена
 * @param currentMiiName имя, на которое указываем.
 */
case class MiiSearchPtr(dkey:String, currentMiiName:String) extends MiiFileT {
  val prefix: String = MiiSearchPtr.prefix
  @JsonIgnore def name: String  = MiiSearchPtr.name
  override def filename: String = name

  /**
   * Сохранить текущий указатель в хранилище.
   * @return Сохраненный экземпляр класса.
   */
  def save: MiiSearchPtr = {
    val os = fs.create(filepath, true)
    try {
      os.writeChars(currentMiiName)
      this

    } finally {
      os.close()
    }
  }
}
