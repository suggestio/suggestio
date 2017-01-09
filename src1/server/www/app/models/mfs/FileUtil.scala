package models.mfs

import java.io.{File, FileInputStream}

import net.sf.jmimemagic.{Magic, MagicMatch, MagicMatchNotFoundException}
import org.apache.commons.codec.digest.DigestUtils
import util.PlayMacroLogsImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 11:49
 * Description: Утиль для работы с файлами в файловой системе.
 */
object FileUtil extends PlayMacroLogsImpl {

  /**
   * Рассчитать чек-сумму, которую можно использовать для ETag например.
    *
    * @param file Исходный файл.
   * @return Строка чексуммы вида "bf35fa420d3e0f669e27b337062bf19f510480d4".
   * @see Написано по мотивам [[http://stackoverflow.com/a/2932513]].
   */
  def sha1(file: File): String = {
    val is = new FileInputStream(file)
    try {
      DigestUtils.sha1Hex(is)
    } finally {
      is.close()
    }
  }


  def getMimeMatch(file: File): Option[MagicMatch] = {
    try {
      Option(Magic.getMagicMatch(file, false, true))
    } catch {
      case mmnfe: MagicMatchNotFoundException =>
        LOGGER.warn(s"getMimeMatch($file): Unable to get MIME from file", mmnfe)
        None
    }
  }

}
