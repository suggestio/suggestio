package io.suggest.sax

import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.Attributes
import org.apache.tika.metadata.Metadata

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.06.13 10:09
 * Description: Валидация сайта через мета-тег. Скорее всего класс будет не нужен, ибо tika умеет сама
 * отправлять метаданные в собственный аккамулятор.
 */
class SioMetaVerificationDetectorSAX extends DefaultHandler {

  import SioMetaVerificationDetectorSAX._

  protected var _acc = List[String]()

  /**
   * Вернуть текущий аккамулятор. Обычно вызывается после парсинга.
   * @return аккамулятор в виде списка строк, в т.ч. пустой.
   */
  def getAcc = _acc

  /**
   * Начало какого-то тега.
   * @param uri
   * @param localName имя тега
   * @param qName
   * @param attributes аттрибуты тега
   */
  override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
    if (localName == "meta") {
      val maybeName = attributes.getValue("name") match {
        // Должно же быть имя у мета-тега
        case null =>
          // Идиоты из фейсбука зачем-то ввели своё имя атрибута для meta-тегов (в рамках ogp.me)
          attributes.getValue("property")

        case str:String => str
      }
      if (maybeName != null && metaNameRe.pattern.matcher(maybeName).matches()) {
        // Да, это мета-тег, относящийся к верификации suggest.io. Нужно извлечь значение и закинуть в аккамулятор.
        val value = attributes.getValue("content")
        if (value != null)
          _acc = value :: _acc
      }
    }
  }

}

object SioMetaVerificationDetectorSAX {
  val metaNameRe = "^\\s*(?i)s[au]gg?est+.io[-|\\s/\\\\,:.](verif(y|icat(e|y|ion))|validat(e|ion))\\s*$".r

  /**
   * Поиск вхождений сабжа в метаданных tika.
   * @param md Метаданные после прогона tika над документом.
   * @return Список найденных строк-значений валидации (возможно пустых/некорректных), без \s по краям.
   */
  def findInTikaMetadata(md: Metadata): List[String] = {
    md.names().foldLeft[List[String]](Nil) { (acc, key) =>
      if(metaNameRe.pattern.matcher(key).matches()) {
        md.get(key).trim :: acc
      } else {
        acc
      }
    }
  }

}
