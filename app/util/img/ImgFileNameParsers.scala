package util.img

import java.util.UUID

import io.suggest.img.ImgCropParsers
import io.suggest.util.UuidUtil
import models.im.{AbsCropOp, ImOp}

import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.10.14 17:21
 * Description: Набор парсеров для нужд декодирования filename'ов и прочих нужд моделей.
 */


trait ImgFileNameParsers extends JavaTokenParsers with ImgCropParsers {

  /** Парсер rowKey из filename: */
  def uuidStrP: Parser[String] = "[a-zA-Z0-9_-]{21,25}".r
  def uuidP: Parser[UUID] = {
    uuidStrP ^^ UuidUtil.base64ToUuid
  }

  /** Парсер списка dynImg-аргументов, сериализованного в виде qs-строки. */
  def dynImgArgsP: Parser[List[ImOp]] = {
    "[^/?]+".r ^^ { qsStr =>
      ImOp.bindImOps("", qsStr)
        .toList
    }
  }

  /** Парсер qs-хвоста имени файла. */
  def dynImgArgsQsP: Parser[List[ImOp]] = {
    "[~?]".r ~> dynImgArgsP
  }

  /** Старый формат хранимого filename'а состоял из id оригинала и оригинального кропа в абсолютных пикселях.
    * Эти два элемента разделялись тильдой. */
  def compatCropSuf = "~" ~> cropStrP

  /** Старый формат кропа можно привести к списку im-трансформаций. */
  def compatCropSuf2ImArgsP: Parser[List[ImOp]] = {
    compatCropSuf ^^ { crop =>
      List(AbsCropOp(crop))
    }
  }

  def dynImgArgsQsEmptyP: Parser[List[ImOp]] = {
    ("?" | "") ^^^ Nil
  }

  /** Парсер filename'а. */
  def fileNameP = uuidP ~ (dynImgArgsQsP | compatCropSuf2ImArgsP | dynImgArgsQsEmptyP)

}
