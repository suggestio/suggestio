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

  // Корневая папка
  val ROOT_DIR = "picture"

  // Директория для складывания файлов, приготовленных для кадрирования
  val TEMP_SUBDIR = "tmp"
  val TEMP_DIR = {
    val d = ROOT_DIR + "/" + TEMP_SUBDIR
    new File(d).mkdirs()
    d
  }
  private val TEMP_DIR_F = new File(TEMP_DIR)

  private val deleteTmpAfterMs = current.configuration.getInt("picture.temp.delete_after_minutes").getOrElse(60).minutes.toMillis

  private val GET_RND_RE = "[0-9]+".r


  /**
   * Приготовиться к отправке файла во временное хранилище, сгенерив путь до него.
   * Будет адрес на несуществующий файл вида picture/tmp/234512341234123412341234.jpg
   */
  def getForTempFile(tempfile: TemporaryFile) = {
    val file = tempfile.file
    val key  = GET_RND_RE.findFirstIn(file.getName).get
    MPictureTmp(key)
  }


  /** Удалить файлы, которые старше deleteTmpAfterMs. Обычно вызывается из util.Cron по таймеру. */
  def cleanupOld() {
    val epoch = System.currentTimeMillis() - deleteTmpAfterMs
    TEMP_DIR_F.listFiles().foreach { f =>
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
      None
    } else {
      val mptmp = MPictureTmp(key)
      if (mptmp.file.isFile)
        Some(mptmp)
      else
        None
    }
  }

  def isKeyValid(key: String) = GET_RND_RE.pattern.matcher(key).matches()

}

import MPictureTmp._

case class MPictureTmp(key: String) {
  if (!isKeyValid(key))
    throw new IllegalStateException("Invalid image key: " + key)

  def relPath = TEMP_DIR + "/" + key + ".jpg"
  lazy val file = current.getFile(relPath)
}

