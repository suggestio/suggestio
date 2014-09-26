package util.img

import java.io.{FileInputStream, File, InputStream}
import org.apache.batik.dom.svg.SAXSVGDocumentFactory
import org.apache.batik.util.XMLResourceDescriptor

import util.PlayMacroLogsImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.09.14 11:45
 * Description: Утиль для работы с svg.
 */
object SvgUtil extends PlayMacroLogsImpl {

  import LOGGER._

  def isSvgFileValid(f: File): Boolean = {
    val is = new FileInputStream(f)
    try {
      isSvgValid(is)
    } finally {
      is.close()
    }
  }

  /**
   * Проверка svg-файла на валидность.
   * @param is Данные.
   * @return true, если svg валиден, иначе false.
   */
  def isSvgValid(is: InputStream): Boolean = {
    try {
      val parser = XMLResourceDescriptor.getXMLParserClassName
      val factory = new SAXSVGDocumentFactory(parser)
      val doc = factory.createDocument("http://suggest.io/test.svg", is)
      // TODO Добавить ещё проверки для распарсенного документа?
      doc != null
    } catch {
      case ex: Throwable =>
        warn("isSvgValid(): Invalid svg data", ex)
        false
    }
  }

}
