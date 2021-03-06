package functional

import java.io.InputStream

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.14 15:27
 * Description: Утиль для ускоренения тестописания.
 */
trait WithInputStream {

  /** Директория с ресурсами. "/util/geo/osm/" например.  */
  val RES_DIR: String

  def getResourceAsStream(fn: String) = getClass.getResource(RES_DIR + fn).openStream()

  protected def withFileStream(fn: String)(f: InputStream => Any): Unit = {
    val is = getResourceAsStream(fn)
    try {
      f(is)
    } finally {
      is.close()
    }
  }

}
