package util.adr.phantomjs

import java.io.{File, FileWriter}

import com.google.inject.{Inject, Singleton}
import com.google.inject.assistedinject.Assisted
import models.MImgSizeT
import models.adr.IAdRenderArgs
import models.im.{OutImgFmt, OutImgFmts}
import models.mproj.ICommonDi
import util.adr.{IAdRrr, IAdRrrDiFactory, IAdRrrUtil}
import views.txt.js.phantom.renderOneAdJs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.08.16 18:59
  * Description: Реализация ad-рендерера на базе Phantom.js.
  * ЕМНИП, Фантом начал использоваться из-за проблем со сглаживанием webfonts в wkhtmltoimage на сервере.
  */
@Singleton
class PhantomJsRrrUtil extends IAdRrrUtil {

  /** Дефолтовое значение quality, если не задано. */
  override def qualityDflt(scrSz: MImgSizeT, fmt: OutImgFmt): Option[Int] = {
    fmt match {
      case OutImgFmts.JPEG => Some(94)
      case _               => super.qualityDflt(scrSz, fmt)
    }
  }

}

/** Интерфейс Guice DI-factory инстансов [[PhantomJsRrr]]. */
trait PhantomJsRrrDiFactory extends IAdRrrDiFactory {
  override def instance(args: IAdRenderArgs): PhantomJsRrr
}

class PhantomJsRrr @Inject() (
  @Assisted override val args : IAdRenderArgs,
  override val mCommonDi      : ICommonDi
)
  extends IAdRrr
{

    /**
   * Запись скрипта в файле.
   * Из-за бага [[https://github.com/ariya/phantomjs/issues/10619]], нужно размеры форсировать в нескольких местах.
   * @param scriptFile Файл скрипта.
   * @param outFile Файл для результирующей картинки.
   * @see [[http://phantomjs.org/api/webpage/method/render.html]]
   */
  private def writeScript(scriptFile: File, outFile: File): Unit = {
    val rendered = renderOneAdJs(args, outFile.getAbsolutePath)
    val w = new FileWriter(scriptFile)
    try {
      w.write(rendered.body)
    } finally {
      w.close()
    }
  }

  /** Синхронный вызов рендера. Нужно создать скрипт для рендера, сохранить его в файл
    * и вызвать "phantomjs todo.js". */
  override def renderSync(dstFile: File): Unit = {
    val scriptFile = File.createTempFile("phantomjs-ad-render", ".js")
    try {
      // Сгенерить скрипт
      writeScript(scriptFile, outFile = dstFile)
      // Запустить phantomjs
      val cmdArgs = Array("phantomjs", scriptFile.getAbsolutePath)
      exec(cmdArgs)
    } finally {
      scriptFile.delete()
    }
  }

}
