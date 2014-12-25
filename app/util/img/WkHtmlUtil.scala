package util.img

import java.io.File
import java.nio.file.Files

import util.{AsyncUtil, PlayMacroLogsImpl}
import models.im._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.14 9:57
 * Description: Утиль для работы с wkhtml2image, позволяющая рендерить html в растровые картинки.
 */

object WkHtmlUtil extends PlayMacroLogsImpl {

  import LOGGER._

  /**
   * Упрощенная функция для того, чтобы быстро сделать всё круто:
   * - асинхронно
   * - с участием кеша для защиты от DDOS.
   * @param args настройки вызова.
   * @return Фьючерс с прочитанной в память картинкой.
   */
  // TODO Этот метод - эталонный quick and dirty быдлокод. Отрендеренные картинки нужно хранить на винче, не трогая кеш.
  def html2imgSimple(args: WkHtmlArgs): Future[Array[Byte]] = {
    // TODO добавить cache
    val dstFile = File.createTempFile("/tmp/", "." + args.outFmt.name)
    val fut = Future {
      html2img(args, dstFile)
      Files.readAllBytes(dstFile.toPath)
    }(AsyncUtil.singleThreadIoContext)
    fut onComplete { case _ =>
      dstFile.delete()
    }
    fut
  }

  /**
   * Запуск конвертации блокировано.
   * @param args Аргументы вызова.
   * @return Массив байт с картинкой если всё ок.
   *         RuntimeException если вызов wkhtmltoimage вернул не 0.
   *         Exception при иных проблемах.
   */
  def html2img(args: WkHtmlArgsT, dstFile: File): Unit = {
    val cmdargs = args.toCmdLine()
    val now = System.currentTimeMillis()
    // TODO Асинхронно запускать сие?
    val p = Runtime.getRuntime.exec(cmdargs.toArray)
    val result = p.waitFor()
    val tookMs = System.currentTimeMillis() - now
    lazy val cmd = cmdargs.mkString(" ")
    trace(cmd + "  ===>>>  " + result + " ; took = " + tookMs + "ms")
    if (result != 0) {
      throw new RuntimeException(s"Cannot execute shell command (result: $result) : $cmd")
    }
  }



}

