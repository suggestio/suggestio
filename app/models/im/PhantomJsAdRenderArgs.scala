package models.im

import java.io.{FileWriter, File}

import models.MImgSizeT
import util.PlayMacroLogsDyn
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.03.15 18:04
 * Description: Параметры и логика вызова рендера карточки через phantom.js.
 */

object PhantomJsAdRenderArgs extends IAdRendererCompanion {

  /** Почему-то viewport не понимает кол-во пикселей правильно.
    * @see [[https://github.com/ariya/phantomjs/issues/10619]] */
  val HEIGHT_FIX = configuration.getInt("ad.render.phantom.height.fixBy") getOrElse -4

  override def forArgs(src: String, scrSz: MImgSizeT, quality: Option[Int], outFmt: OutImgFmt): IAdRenderArgs = {
    apply(src = src,  scrSz = scrSz,  outFmt = outFmt,  quality = quality)
  }
}


import PhantomJsAdRenderArgs._


trait PhantomJsAdRenderArgsT extends IAdRenderArgsSyncFile with PlayMacroLogsDyn {

  /**
   * Запись скрипта в файле.
   * @param scriptFile Файл скрипта.
   * @param outFile Файл для результирующей картинки.
   * @see [[http://phantomjs.org/api/webpage/method/render.html]]
   */
  def writeScript(scriptFile: File, outFile: File): Unit = {
    val w = new FileWriter(scriptFile)
    try {
      import w.write
      write("var page = require('webpage').create();")
      // Записать размер окна
      write("page.viewportSize = {width:")
      val sz = scrSz
      write(sz.width.toString)
      write(",height:")
      write(sz.height.toString + HEIGHT_FIX)
      write("};")
      // Записать команды для открытия ссылки
      write("page.open('")
      write(src)
      write("', function() {")
      // Записать команды для рендера
      write("page.render('")
      write(outFile.getAbsolutePath)
      // Записать формат выходной картинки.
      write("', {format: '")
      write(outFmt.name)
      // Поддержка quality здесь: quality: 0..100
      write("'")
      quality.foreach { q =>
        write(", quality:")
        write(q.toString)
      }
      write("});")
      // Записать команду завершения phantom.js
      write("phantom.exit();")
      // Всё закрыть все скобки
      write("});\n")
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


/** Дефолтовая реализация рендера через phantomjs. */
case class PhantomJsAdRenderArgs(
  src         : String,
  scrSz       : MImgSizeT,
  outFmt      : OutImgFmt         = AdRenderArgs.OUT_FMT_DFLT,
  quality     : Option[Int]       = None
) extends PhantomJsAdRenderArgsT

