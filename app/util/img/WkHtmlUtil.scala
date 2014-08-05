package util.img

import java.io.File
import java.net.URL
import org.apache.commons.io.FileUtils
import util.PlayMacroLogsImpl
import util.img.OutImgFmts.OutImgFmt
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.14 9:57
 * Description: Утиль для работы с wkhtml2image, позволяющая рендерить html в растровые картинки.
 */
object WkHtmlUtil extends PlayMacroLogsImpl {

  import LOGGER._

  /** path для директории кеширования. */
  val CACHE_DIR: Option[String] = {
    val dirPath = configuration.getString("wkhtml.cache.dir") getOrElse "/tmp/sio2/wkhtml/cache"
    val dirFile = new File(dirPath)
    if ((dirFile.exists && dirFile.isDirectory)  ||  dirFile.mkdirs) {
      debug("WkHtml cache dir is set to " + dirPath)
      Some(dirPath)
    } else {
      warn("WkHtml cache dir in not created/unset or invalid. Working without cache.")
      None
    }
  }

  /** Команда wkhtmltoimage для отправки на выполнение. */
  val CMD: List[String] = {
    var l = List(
      "--disable-plugins",
      "--images",
      "--zoom", "2.0",
      "--width", "1280",
      "--height", "1600",
      "--quality", "100"
    )
    l = CACHE_DIR.fold (l) ("--cache-dir" :: _ :: l)
    "wkhtmltoimage" :: l
  }

  /**
   * Запуск конвертации блокировано.
   * @param url Ссылка для получения страницы.
   * @param outFmt Выходной формат картинки.
   * @return Массив байт с картинкой если всё ок.
   *         RuntimeException если вызов wkhtmltoimage вернул не 0.
   *         Exception при иных проблемах.
   */
  def html2img(url: URL, outFmt: OutImgFmt): Array[Byte] = {
    val tmpFile = File.createTempFile("wkhtmltoimage", "." + outFmt)
    try {
      val cmdargs = CMD ++ List(url.toExternalForm, tmpFile.getAbsolutePath)
      val p = Runtime.getRuntime.exec(cmdargs.toArray)
      val result = p.waitFor()
      lazy val cmd = cmdargs.mkString(" ")
      trace(cmd + "  ===>>>  " + result)
      if (result == 0) {
        FileUtils.readFileToByteArray(tmpFile)
      } else {
        throw new RuntimeException(s"Cannot execute shell command (result: $result) : $cmd")
      }
    } finally {
      tmpFile.delete()
    }
  }

}

