package models.im

import java.io.{FileWriter, File}

import models.MImgSizeT
import util.PlayMacroLogsDyn
import views.txt.js.phantom._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.03.15 18:04
 * Description: Параметры и логика вызова рендера карточки через phantom.js.
 */

object PhantomJsAdRenderArgs extends IAdRendererCompanion {


  /** Дефолтовое значение quality, если не задано. */
  override def qualityDflt(scrSz: MImgSizeT, fmt: OutImgFmt): Option[Int] = {
    fmt match {
      case OutImgFmts.JPEG => Some(94)
      case _               => super.qualityDflt(scrSz, fmt)
    }
  }

  override def forArgs(src: String, scrSz: MImgSizeT, outFmt: OutImgFmt, quality: Option[Int] = None): IAdRenderArgs = {
    apply(src = src,  scrSz = scrSz,  outFmt = outFmt,  quality = quality)
  }
}


/** Логика работы активной части модели вынесена в трейт. */
trait PhantomJsAdRenderArgsT extends IAdRenderArgsSyncFile with PlayMacroLogsDyn {

  /**
   * Запись скрипта в файле.
   * Из-за бага [[https://github.com/ariya/phantomjs/issues/10619]], нужно размеры форсировать в нескольких местах.
   * @param scriptFile Файл скрипта.
   * @param outFile Файл для результирующей картинки.
   * @see [[http://phantomjs.org/api/webpage/method/render.html]]
   */
  def writeScript(scriptFile: File, outFile: File): Unit = {
    val rendered = renderOneAdJs(this, outFile.getAbsolutePath)
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


/** Дефолтовая реализация абстрактной модели [[PhantomJsAdRenderArgsT]],
  * которая предоставляет рендер через phantomjs. */
case class PhantomJsAdRenderArgs(
  src         : String,
  scrSz       : MImgSizeT,
  outFmt      : OutImgFmt,
  quality     : Option[Int]
) extends PhantomJsAdRenderArgsT

