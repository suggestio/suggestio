package util.img

import java.util.UUID

import io.suggest.img.{ImgCropParsers, MImgFormat, MImgFormats}
import io.suggest.primo.TypeT
import io.suggest.util.UuidUtil
import io.suggest.util.logs.MacroLogsDyn
import models.im.{AbsCropOp, ImOp}

import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.10.14 17:21
 * Description: Набор парсеров для нужд декодирования filename'ов и прочих нужд моделей.
 */

trait ImgFileNameParsers extends JavaTokenParsers with ImgCropParsers with MacroLogsDyn {

  /** Парсер rowKey из filename: */
  def uuidStrP: Parser[String] = "[a-zA-Z0-9_-]{21,25}".r

  /** Парсер rowKey, перводящий id в uuid. */
  def uuidP: Parser[UUID] = {
    uuidStrP ^^ UuidUtil.base64ToUuid
  }
    
  /** Парсер списка dynImg-аргументов, сериализованного в виде qs-строки. */
  def dynImgArgsP: Parser[List[ImOp]] = {
    "[^/?]*".r ^^ { qsStr =>
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

  /** Парсер строки данных трансформации картинки.
    * Порядок имеет значение. compat, пока нужен, не должен конфликтовать с dynImgArgsQsP. */
  def imOpsP: Parser[List[ImOp]] = {
    compatCropSuf2ImArgsP | dynImgArgsQsP | dynImgArgsQsEmptyP
  }

  /** Парсер формата картинки. */
  def imgFormatP: Parser[MImgFormat] = {
    s"[a-z]{${MImgFormats.NAME_LEN_MIN},${MImgFormats.NAME_LEN_MAX}}".r
      .map { fileExt =>
        MImgFormats.withFileExt(fileExt)
      }
      .filter(_.isDefined)
      .map(_.get)
  }

  /** Парсер формата картинки с точкой в начале: .jpeg */
  def dotImgFormatP = "." ~> imgFormatP
  def dotImgFormatOptP = opt( dotImgFormatP )
  /** Опциональный парсер формата. */
  def dotImgFormatOrJpegP = opt(dotImgFormatP) ^^ { _.getOrElse {
    LOGGER.warn("dotDynFormatOrJpegP(): no fmt, using JPEG...")
    MImgFormats.JPEG
  }}

  /** Парсер полного filename'а. */
  def fileNameP = uuidStrP ~ imOpsP

  def parseImgArgs(imOpsStr: String) = parseAll(dynImgArgsP, imOpsStr)

}

/** Реализация [[ImgFileNameParsers]]. */
abstract class ImgFileNameParsersImpl extends ImgFileNameParsers with TypeT {

  def fileName2miP: Parser[T]

  def fromFileName(filename: String): ParseResult[T] = {
    parseAll(fileName2miP, filename)
  }

}
