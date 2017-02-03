package io.suggest.svg

import java.io.{File, FileInputStream, InputStream}

import io.suggest.util.logs.MacroLogsImpl
import org.apache.batik.dom.svg.SAXSVGDocumentFactory
import org.apache.batik.util.XMLResourceDescriptor

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.09.14 11:45
 * Description: Утиль для работы с svg.
 */
object SvgUtil extends MacroLogsImpl {

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


  /**
   * Может ли указанные mime-тип быть svg? Это нужно для исправления JMimeMagic.
   * @param mime MIME-type.
   * @return true, если svg внутри допустим. Иначе false.
   */
  def maybeSvgMime(mime: String): Boolean = {
    mime.startsWith("text/") || mime.startsWith("image/svg")
  }

}
