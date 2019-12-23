package models.im

import javax.inject.{Inject, Singleton}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.suggest.common.geom.d2.ISize2di
import io.suggest.fio.IDataSource
import io.suggest.img.ImgSzDated
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.{ICommonDi, IMCommonDi}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 18:27
 * Description: Интерфейс, унифицирующий различные над-модели картинок:
 * - локальные картинки на ФС: [[MLocalImg]].
 * - удалённые permanent-хранилища на кластере: [[MImg3]].
 */
trait MAnyImgT {

  type MImg_t <: MImgT

  /** Инстанс локальной картинки. Сама картинка может не существовать. */
  def toLocalInstance: MLocalImg

  /** Вернуть инстанс над-модели MImg. */
  def toWrappedImg: MImg3

  /** Инстанс для доступа к картинке без каких-либо наложенных на неё изменений. */
  def original: MAnyImgT

  val dynImgId: MDynImgId

  def withDynImgId(dynImgId: MDynImgId): MAnyImgT

}



/** Трейт для статических частей img-моделей. */
trait MAnyImgsT[T <: MAnyImgT] extends IMCommonDi {

  import mCommonDi.ec

  /** Удалить картинку из модели/моделей. */
  def delete(mimg: T): Future[_]

  /** Подготовить локальный файл с картинкой. */
  def toLocalImg(mimg: T): Future[Option[MLocalImg]]

  /** Асинхронно стримить картинку из хранилища, прочитав связанные с ней данные. */
  def getDataSource(mimg: T): Future[IDataSource]

  /** Асинхронно стримить картинку из хранилища. */
  final def getStream(mimg: T): Source[ByteString, _] = {
    val srcFut = for (ds <- getDataSource(mimg)) yield {
      ds.data
    }
    Source.fromFutureSource(srcFut)
  }

  /** Получить ширину и длину картинки. */
  def getImageWH(mimg: T): Future[Option[ISize2di]]

  def rawImgMeta(mimg: T): Future[Option[ImgSzDated]]

}


/** Статическая над-модель, реализующая разные общие методы для любых картинок. */
@Singleton
class MAnyImgs @Inject() (
                           mLocalImgs               : MLocalImgs,
                           mImgs3                   : MImgs3,
                           override val mCommonDi   : ICommonDi
                         )
  extends MAnyImgsT[MAnyImgT]
  with MacroLogsImpl
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

  /** Асинхронно стримить картинку из хранилища, прочитав связанные с ней данные. */
  override def getDataSource(mimg: MAnyImgT): Future[IDataSource] = {
    mimg match {
      case mimg3: MImg3 =>
        mImgs3.getDataSource(mimg3)
      case localImg: MLocalImg =>
        mLocalImgs.getDataSource(localImg)
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

  override def rawImgMeta(mimg: MAnyImgT): Future[Option[ImgSzDated]] = {
    mimg match {
      case mimg3: MImg3 =>
        mImgs3.rawImgMeta(mimg3)
      case localImg: MLocalImg =>
        mLocalImgs.rawImgMeta(localImg)
    }
  }

}

