package models

import java.io.File
import play.api.libs.Files.TemporaryFile
import play.api.Play.current
import concurrent.duration._
import util.img.OutImgFmts, OutImgFmts.OutImgFmt

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

  private val GET_RND_RE = "([0-9]{16,22})".r

  TEMP_DIR.mkdirs()

  /** Префикс для ключей модели. Нужно чтобы различать tmpid'шники от других моделей
    * (от [[io.suggest.model.MUserImgOrig]] в частности) */
  val KEY_PREFIX = "itmp-"

  val FILENAME_RE = (KEY_PREFIX + GET_RND_RE + "([a-zA-Z0-9]+)?\\.([a-z]+)").r


  /** Источник идентификаторов в модели. */
  private def getKeyFromUploadedTmpfileName(fn: String) = GET_RND_RE.findFirstIn(fn).get

  /** Дефолтовый формат картинки. */
  def fmtDflt = OutImgFmts.JPEG

  /**
   * Приготовиться к отправке файла во временное хранилище, сгенерив путь до него. По сути враппер на apply().
   * Будет адрес на несуществующий файл вида picture/tmp/234512341234123412341234.jpg
   */
  def getForTempFile(tempfile: TemporaryFile, outFmt: OutImgFmt = fmtDflt, marker: Option[String] = None): MPictureTmp = {
    val file = tempfile.file
    val key  = getKeyFromUploadedTmpfileName(file.getName)
    val newTmpFilename = mkFilename(key, outFmt, marker)
    MPictureTmp(newTmpFilename)
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
   * @param filename ключ временного хранилища.
   * @return Option[File]
   */
  def find(filename: String): Option[MPictureTmp] = {
    if (isFilenameValid(filename)) {
      val mptmp = MPictureTmp(filename)
      if (mptmp.isExist) Some(mptmp) else None
    } else {
      None
    }
  }


  def isFilenameValid(serId: String) = {
    FILENAME_RE.pattern.matcher(serId).matches()
  }


  /** Опциональная версия apply. */
  def maybeApply(filename: String): Option[MPictureTmp] = {
    if (isFilenameValid(filename))
      Some(MPictureTmp(filename))
    else
      None
  }

  def mkFilename(key: String, fmt: OutImgFmt = fmtDflt, marker: Option[String] = None): String = {
    val markerStr = marker getOrElse ""
    val filename = KEY_PREFIX + key + markerStr + "." + fmt
    if (isFilenameValid(filename))
      filename
    else
      throw new IllegalArgumentException("Invalid filename: " + filename)
  }

}

import MPictureTmp._

case class MPictureTmp(filename: String) {

  // Все параметры разом.
  val (key, fmt, markerOpt) = {
    val Some(List(_key, markerStrOrNull, fmtStr)) = FILENAME_RE.unapplySeq(filename)
    val _fmt = if(fmtStr.isEmpty)  fmtDflt  else  OutImgFmts.withName(fmtStr)
    val _marker = if (markerStrOrNull == null || markerStrOrNull.isEmpty) None else Some(markerStrOrNull)
    (_key, _fmt, _marker)
  }

  val file = {
    new File(TEMP_DIR, filename)
  }

  def isExist = file.isFile

}

