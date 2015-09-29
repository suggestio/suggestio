package models.mfs

import java.io.{FileInputStream, File}

import net.sf.jmimemagic.Magic
import org.apache.commons.codec.digest.DigestUtils

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 11:49
 * Description: Утиль для работы с файлами в файловой системе.
 */
object FileUtil {

  /**
   * Рассчитать чек-сумму, которую можно использовать для ETag например.
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


  def getMimeMatch(file: File) = Option( Magic.getMagicMatch(file, false, true) )

}
