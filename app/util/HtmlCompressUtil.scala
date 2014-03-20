package util

import com.googlecode.htmlcompressor.compressor.HtmlCompressor
import play.api.Play, Play.current

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.03.14 10:18
 * Description: Утиль для сжатия HTML-ответов.
 */
object HtmlCompressUtil {

  // Для сжатия инлайновых css/js блоков надо переместить их в соответствующие файлы в /app/assets/.
  // Включение сжатия css/js прямо здесь приведёт к тормозам.
  val compressor = new HtmlCompressor()
  compressor.setPreserveLineBreaks(Play.isDev)
  compressor.setRemoveComments(!Play.isDev)
  compressor.setRemoveIntertagSpaces(true)
  compressor.setRemoveHttpProtocol(true)
  compressor.setRemoveHttpsProtocol(true)

}

