package models.im

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import models.mproj.ICommonDi
import models.{IImgMeta, ISize2di, ImgCrop}
import play.api.libs.iteratee.Enumerator
import util.PlayMacroLogsImpl

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 18:27
 * Description: Интерфейс, унифицирующий различные над-модели картинок:
 * - локальные картинки на ФС: [[MLocalImg]].
 * - удалённые permanent-хранилища на кластере: [[MImg3]].
 */
trait MAnyImgT extends ImgFilename with DynImgOpsString {

  type MImg_t <: MImgT

  /** Ключ ряда картинок, id для оригинала и всех производных. */
  def rowKey: UUID

  /** Инстанс локальной картинки. Сама картинка может не существовать. */
  def toLocalInstance: MLocalImg

  /** Вернуть инстанс над-модели MImg. */
  def toWrappedImg: MImg3

  /** Инстанс для доступа к картинке без каких-либо наложенных на неё изменений. */
  def original: MAnyImgT

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



/** Трейт для статических частей img-моделей. */
trait MAnyImgsT[T <: MAnyImgT] {

  /** Удалить картинку из модели/моделей. */
  def delete(mimg: T): Future[_]

  /** Подготовить локальный файл с картинкой. */
  def toLocalImg(mimg: T): Future[Option[MLocalImg]]

  /** Асинхронно стримить картинку из хранилища. */
  def getStream(mimg: T): Enumerator[Array[Byte]]

  /** Получить ширину и длину картинки. */
  def getImageWH(mimg: T): Future[Option[ISize2di]]

  def rawImgMeta(mimg: T): Future[Option[IImgMeta]]

}


/** Статическая над-модель, реализующая разные общие методы для любых картинок. */
@Singleton
class MAnyImgs @Inject() (
  mLocalImgs  : MLocalImgs,
  mImgs3      : MImgs3,
  mCommonDi   : ICommonDi
)
  extends MAnyImgsT[MAnyImgT]
  with PlayMacroLogsImpl
{

  import mCommonDi._

  /** Удалить картинку из всех img-моделей. */
  override def delete(mimg: MAnyImgT): Future[_] = {
    // Запустить параллельное удаление из всех моделей.
    val remoteDelFut = mImgs3.delete( mimg.toWrappedImg )
    val localDelFut = mLocalImgs.delete( mimg.toLocalInstance )
    // Дожидаемся всех фьючерсов удаления...
    localDelFut
      .flatMap(_ => remoteDelFut)
  }

  override def toLocalImg(mimg: MAnyImgT): Future[Option[MLocalImg]] = {
    mimg match {
      case mimg3: MImg3 =>
        mImgs3.toLocalImg(mimg3)
      case localImg: MLocalImg =>
        mLocalImgs.toLocalImg(localImg)
    }
  }

  override def getStream(mimg: MAnyImgT): Enumerator[Array[Byte]] = {
    mimg match {
      case mimg3: MImg3 =>
        mImgs3.getStream(mimg3)
      case localImg: MLocalImg =>
        mLocalImgs.getStream(localImg)
    }
  }

  override def getImageWH(mimg: MAnyImgT): Future[Option[ISize2di]] = {
    mimg match {
      case mimg3: MImg3 =>
        mImgs3.getImageWH(mimg3)
      case localImg: MLocalImg =>
        mLocalImgs.getImageWH(localImg)
    }
  }

  override def rawImgMeta(mimg: MAnyImgT): Future[Option[IImgMeta]] = {
    mimg match {
      case mimg3: MImg3 =>
        mImgs3.rawImgMeta(mimg3)
      case localImg: MLocalImg =>
        mLocalImgs.rawImgMeta(localImg)
    }
  }

}

