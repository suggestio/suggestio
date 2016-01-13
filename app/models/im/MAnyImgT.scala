package models.im

import java.util.UUID

import models.{ISize2di, IImgMeta, ImgCrop}
import util.PlayMacroLogsI

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 18:27
 * Description: Интерфейс, унифицирующий различные над-модели картинок:
 * - локальные картинки на ФС: [[MLocalImg]].
 * - удалённые permanent-хранилища на кластере: [[MImg3]].
 */
trait MAnyImgT extends PlayMacroLogsI with ImgFilename with DynImgOpsString {

  type MImg_t <: MImgT

  /** Ключ ряда картинок, id для оригинала и всех производных. */
  def rowKey: UUID

  /** Вернуть локальный инстанс модели с файлом на диске. */
  def toLocalImg: Future[Option[MLocalImgT]]

  /** Вернуть инстанс над-модели MImg. */
  def toWrappedImg: MImg_t

  /** Получить ширину и длину картинки. */
  def getImageWH: Future[Option[ISize2di]]

  /** Инстанс для доступа к картинке без изменений. */
  def original: MAnyImgT

  def rawImgMeta: Future[Option[IImgMeta]]

  /** Нащупать crop. Используется скорее как compat к прошлой форме работы с картинками. */
  def cropOpt: Option[ImgCrop] = {
    val iter = dynImgOps
      .iterator
      .flatMap {
        case AbsCropOp(crop) => Seq(crop)
        case _ => Nil
      }
    if (iter.hasNext)
      Some(iter.next())
    else
      None
  }

  def isCropped: Boolean = {
    dynImgOps
      .exists { _.isInstanceOf[ImCropOpT] }
  }

  def delete: Future[_]

}


/** Поле filename для класса. */
trait ImgFilename {
  def rowKeyStr: String
  def hasImgOps: Boolean
  def dynImgOpsStringSb(sb: StringBuilder): StringBuilder

  def fileName: String = fileNameSb().toString()

  /**
   * Билдер filename-строки
   * @param sb Исходный StringBuilder.
   * @return StringBuilder.
   */
  def fileNameSb(sb: StringBuilder = new StringBuilder(80)): StringBuilder = {
    sb.append(rowKeyStr)
    if (hasImgOps) {
      sb.append('~')
      dynImgOpsStringSb(sb)
    }
    sb
  }
}


/** Поле минимально-сериализованных dynImg-аргументов для класса. */
trait DynImgOpsString {

  def dynImgOps: Seq[ImOp]

  def isOriginal: Boolean = dynImgOps.isEmpty

  def dynImgOpsStringSb(sb: StringBuilder = ImOp.unbindSbDflt): StringBuilder = {
    ImOp.unbindImOpsSb(
      keyDotted = "",
      value = dynImgOps,
      withOrderInx = false,
      sb = sb
    )
  }

  def dynImgOpsString: String = {
    dynImgOpsStringSb().toString()
  }

  // Исторически, опциональный доступ к dynImgOpsString осуществляет метод qOpt.
}
