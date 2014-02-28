package models

import java.io.File
import play.api.libs.Files.TemporaryFile
import play.api.Play.current
import concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.02.14 15:25
 * Description: Временные картинки лежат тут. Сюда попадают промежуточные/сырые картинки, которые подлежат кадрированию
 * или иной дальнейшей обработке. В качестве хранилища используется локальная ФС.
 */
object MPictureTmp {

  // Директория для складывания файлов, приготовленных для кадрирования
  val TEMP_DIR_REL = "picture/tmp"
  val TEMP_DIR = current.getFile(TEMP_DIR_REL)

  private val deleteTmpAfterMs = current.configuration.getInt("picture.temp.delete_after_minutes").getOrElse(60).minutes.toMillis

  private val GET_RND_RE = "[0-9]+".r

  TEMP_DIR.mkdirs()

  /** Префикс для ключей модели. Нужно чтобы различать tmpid'шники от других моделей
    * (от [[io.suggest.model.MUserImgOrig]] в частности) */
  val KEY_PREFIX = "itmp-"

  val KEY_RE = (KEY_PREFIX + GET_RND_RE).r


  /** Источник идентификаторов в модели. */
  def getKeyFromUploadedTmpfileName(fn: String) = KEY_PREFIX + GET_RND_RE.findFirstIn(fn).get

  /**
   * Приготовиться к отправке файла во временное хранилище, сгенерив путь до него.
   * Будет адрес на несуществующий файл вида picture/tmp/234512341234123412341234.jpg
   */
  def getForTempFile(tempfile: TemporaryFile): MPictureTmp = {
    val file = tempfile.file
    val key  = getKeyFromUploadedTmpfileName(file.getName)
    MPictureTmp(key)
  }


  /** Удалить файлы, которые старше deleteTmpAfterMs. Обычно вызывается из util.Cron по таймеру. */
  def cleanupOld() {
    val epoch = System.currentTimeMillis() - deleteTmpAfterMs
    TEMP_DIR.listFiles().foreach { f =>
      if (f.isFile && f.lastModified() <= epoch)
        f.delete()
    }
  }

  /**
   * Вернуть временный файл, если такой имеется.
   * @param key ключ временного хранилища.
   * @return Option[File]
   */
  def find(key: String) : Option[MPictureTmp] = {
    if (isKeyValid(key)) {
      val mptmp = MPictureTmp(key)
      if (mptmp.file.isFile)
        Some(mptmp)
      else
        None
    } else {
      None
    }
  }

  def isExist(key: String): Boolean = {
    if (isKeyValid(key)) {
      val mptmp = MPictureTmp(key)
      mptmp.file.isFile
    } else {
      throw new IllegalArgumentException("Not valid key: " + key)
    }
  }

  /**
   * Корректен ли внешне переданных ключ tmp-картинки?
   * @param key Ключ картинки.
   * @return true, если ключ выглядит верно.
   */
  def isKeyValid(key: String) = {
    KEY_RE.pattern.matcher(key).matches()
  }


  /** Опциональная версия apply. */
  def maybeApply(key: String): Option[MPictureTmp] = {
    if (isKeyValid(key))
      Some(MPictureTmp(key))
    else
      None
  }

}

import MPictureTmp._

case class MPictureTmp(key: String) {
  val file = new File(TEMP_DIR, key + ".jpg")
}

