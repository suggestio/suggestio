package io.suggest.svg

import java.io.{File, FileInputStream, InputStream}

import io.suggest.util.logs.MacroLogsImpl
import org.apache.batik.anim.dom.SAXSVGDocumentFactory
import org.apache.batik.util.XMLResourceDescriptor

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.09.14 11:45
 * Description: Утиль для работы с svg.
 */
object SvgUtil extends MacroLogsImpl {

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
    def logPrefix = s"isSvgValid($is):"
    try {
      val parser = XMLResourceDescriptor.getXMLParserClassName
      val factory = new SAXSVGDocumentFactory( parser )
      val doc = factory.createDocument("http://suggest.io/test.svg", is)
      // TODO Добавить ещё проверки для распарсенного документа?
      val r = doc != null
      if (!r)
        LOGGER.warn(s"$logPrefix doc is empty")
      r
    } catch {
      case ex: Throwable =>
        LOGGER.warn(s"$logPrefix Invalid SVG data", ex)
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
