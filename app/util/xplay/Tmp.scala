package util.xplay

import java.io.File
import org.apache.commons.io.FileUtils
import play.api.{Configuration, Application}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.05.15 18:56
 * Description: Временные файлы должны складываться в одну директорию.
 * При старте эта директория должна пересоздаваться, чтобы удалить оттуда любые забытые остатки с прошлых запусков.
 * Потерянные временные файлы появляются, когда во время какого-то действия с tmp-файлом jvm была отстановлена или
 * произошел какой-то неотработанный сбой в программе или просто логика программы содержит ошибку.
 *
 * Есть также долговременная tmp-директория: long. Её содержимое переживает перезапуски.
 */
object Tmp {

  /** Внутренний кэш для изменябельного значения временной директории. */
  private var _tmpDir: File = null

  private def _fixOpt(vv: Option[String], dflt: => String): String = {
    vv.map { _.trim }
      .filter { !_.isEmpty }
      .getOrElse(dflt)
  }

  private def _getTmpDir(conf: Configuration): File = {
    val ppath = {
      val vv = conf.getString("sio.tmp.short.dir.parent")
        .orElse { Option( System.getenv("TMPDIR") ) }
      _fixOpt(vv, "/tmp")
    }

    val dname = {
      val vv = conf.getString("sio.tmp.short.dir.name")
      _fixOpt(vv, "sio2/short")
    }

    new File(ppath, dname)
  }

  /** Директори для временных файлов. */
  def TMP_DIR = _tmpDir

  /** Выполнить пересоздание директории для временных файлов. В нормер оно вызывается при старте. */
  def recreateTmp(app: Application): Unit = {
    val d = _getTmpDir(app.configuration)
    if (d.exists())
      FileUtils.deleteDirectory(d)
    d.mkdir()
    _tmpDir = d
  }

}
