package util.img

import java.io.File
import java.net.URL
import org.apache.commons.io.FileUtils
import util.PlayMacroLogsImpl
import util.img.OutImgFmts.OutImgFmt

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.14 9:57
 * Description: Утиль для работы с wkhtml2image, позволяющая рендерить html в растровые картинки.
 */
object WkHtmlUtil extends PlayMacroLogsImpl {

  import LOGGER._

  val CMD = List(
    "wkhtmltoimage",
    "--disable-local-file-access",
    "--disable-plugins",
    "--images",
    "--zoom", "2.0",
    "--width", "1280",
    "--height", "1600",
    "--quality", "100"
  )

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
