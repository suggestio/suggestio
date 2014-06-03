package models

import java.io.File
import play.api.Play.{current, configuration}
import concurrent.duration._
import util.img.OutImgFmts, OutImgFmts.OutImgFmt
import util.FormUtil
import org.apache.commons.io.FileUtils

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.02.14 15:25
 * Description: Временные картинки лежат тут. Сюда попадают промежуточные/сырые картинки, которые подлежат кадрированию
 * или иной дальнейшей обработке. В качестве хранилища используется локальная ФС.
 */
object MPictureTmp {
  // Нельзя тут дергать напрямую JavaTokenParsers из-за необходимой совместимости с парсерами ImgCrop.
  // Поэтому тут используется внешняя реализация парсеров.
  import io.suggest.img.ImgUtilParsers._

  // Директория для складывания файлов, приготовленных для кадрирования
  val TEMP_DIR_REL = "picture/tmp"
  val TEMP_DIR = current.getFile(TEMP_DIR_REL)

  /** Префикс для ключей модели. Нужно чтобы различать tmpid'шники от других моделей
    * (от io.suggest.model.MUserImgOrig в частности) */
  val KEY_PREFIX = "itmp-"

  private val deleteTmpAfterMs = configuration.getInt("picture.temp.delete_after_minutes").getOrElse(60).minutes.toMillis

  private val GET_RND_RE = "([0-9]{16,22})".r

  val FMT_PARSER: Parser[OutImgFmt] = {
    OutImgFmts.values
      .iterator
      .map { oif  =>  ("(?i)" + oif).r ^^^ oif }
      .reduceLeft { _ | _ }
  }

  val MARKER_OPT_PARSER: Parser[Option[String]] = {
    val markerParser = "(?i)[a-z0-9]*".r ^^ FormUtil.strTrimF
    opt(markerParser) ^^ { _.filter(!_.isEmpty) }
  }

  val FILENAME_PARSER: Parser[MPictureTmpData] = {
    (KEY_PREFIX ~> GET_RND_RE) ~ MARKER_OPT_PARSER ~ opt("~" ~> ImgCrop.CROP_STR_PARSER) ~ ("." ~> FMT_PARSER) ^^ {
      case key ~ markerOpt ~ cropOpt ~ fmt =>
        MPictureTmpData(key, markerOpt, fmt, cropOpt)
    }
  }

  TEMP_DIR.mkdirs()


  def mkTmpFileSuffix(markerOpt: Option[String] = None,
                      cropOpt: Option[ImgCrop] = None,
                      outFmt: OutImgFmt = OutImgFmts.JPEG,
                      acc: StringBuilder = new StringBuilder) = {
    if (markerOpt.isDefined)
      acc.append(markerOpt.get)
    if (cropOpt.isDefined) {
      acc.append('~').append(cropOpt.get.toUrlSafeStr)
    }
    acc.append('.').append(outFmt.toString)
  }


  def mkNew(markerOpt: Option[String] = None,
            cropOpt: Option[ImgCrop] = None,
            outFmt: OutImgFmt = OutImgFmts.JPEG): MPictureTmp = {
    val tmpFileSuffix = mkTmpFileSuffix(markerOpt, cropOpt, outFmt).toString()
    val tmpFile = File.createTempFile(KEY_PREFIX, tmpFileSuffix, TEMP_DIR)
    getForTempFile(tmpFile, outFmt, markerOpt, cropOpt)
  }


  /** Источник идентификаторов в модели. */
  private def getKeyFromUploadedTmpfileName(fn: String) = GET_RND_RE.findFirstIn(fn).get

  /** Дефолтовый формат картинки. */
  def fmtDflt = OutImgFmts.JPEG

  /**
   * Приготовиться к отправке файла во временное хранилище, сгенерив путь до него. По сути враппер на apply().
   * Будет адрес на несуществующий файл вида picture/tmp/234512341234123412341234.jpg
   */
  def getForTempFile(
    file: File,
    outFmt: OutImgFmt = fmtDflt,
    marker: Option[String] = None,
    cropOpt: Option[ImgCrop] = None
  ): MPictureTmp = {
    val key  = getKeyFromUploadedTmpfileName(file.getName)
    val data = MPictureTmpData(key, marker, outFmt, cropOpt)
    val newTmpFilename = mkFilename(data)
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
    maybeApply(filename).filter(_.isExist)
  }

  def parseFilename(filename: String) = parse(FILENAME_PARSER, filename)


  def isFilenameValid(serId: String) = parseFilename(serId).successful


  /** Опциональная версия apply. */
  def maybeApply(filename: String): Option[MPictureTmp] = {
    val pr = parseFilename(filename)
    if (pr.successful) {
      val mptmp = MPictureTmp(filename, pr.get)
      Some(mptmp)
    } else {
      None
    }
  }

  def mkFilename(data: MPictureTmpData): String = {
    val filename = data.toFilename
    if (isFilenameValid(filename))
      filename
    else
      throw new IllegalArgumentException("Invalid filename: " + filename)
  }


  def apply(filename: String): MPictureTmp = new MPictureTmp(filename)
  def apply(data: MPictureTmpData): MPictureTmp = new MPictureTmp(data)
}

import MPictureTmp._


case class MPictureTmp(filename: String, data: MPictureTmpData) {
  def this(filename: String) = this(filename, parseFilename(filename).get)
  def this(data: MPictureTmpData) = this(data.toFilename, data)

  val file = {
    new File(TEMP_DIR, filename)
  }

  def isExist = file.isFile

  def writeIntoFile(imgBytes: Array[Byte]) {
    FileUtils.writeByteArrayToFile(file, imgBytes)
  }
}


case class MPictureTmpData(key: String, markerOpt: Option[String], fmt: OutImgFmt, cropOpt: Option[ImgCrop]) {
  /** Сериализовать всё добро в имя файла. */
  def toFilename: String = {
    val sb = new StringBuilder(64, KEY_PREFIX)
      .append(key)
    mkTmpFileSuffix(markerOpt, cropOpt, fmt, sb)
      .toString()
  }

  def marker = markerOpt getOrElse ""
}

