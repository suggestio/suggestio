package models.im

import java.nio.ByteBuffer
import java.util.UUID

import io.suggest.model.img.IImgMeta
import io.suggest.model.{MUserImgMeta2, MUserImg2}
import models._
import org.joda.time.DateTime
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumerator
import util.PlayLazyMacroLogsImpl
import util.img.{ImgFileNameParsersImpl, ImgFormUtil}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.10.14 17:36
 * Description: Картинка, хранящаяся где-то в системе.
 * MPictureTmp была заменена ан im.MLocalImg, а недослой ImgIdKey заменен на MImg, использующий MLocalImg как
 * кеш-модель для моделей permanent-хранения (изначально -- кассандра и MUserImg2 и MUserImgMeta2).
 *
 * Эта модель легковесна и полностью виртуальна.
 * Все данные картинок хранятся в локальной ненадежной кеширующей модели и в постояной моделях (cassandra и др.).
 */

object MImg extends PlayLazyMacroLogsImpl with IMImgCompanion { model =>

  override type T = MImg

  /** Реализация парсеров для данной модели. */
  class Parsers extends ImgFileNameParsersImpl {

    override type T = MImg

    /** Парсер filename'ов, выдающий на выходе готовые экземпляры MImg. */
    override def fileName2miP: Parser[MImg] = {
      fileNameP ^^ {
        case uuid ~ imOps  =>  MImg(uuid, imOps)
      }
    }

  }

  /**
   * Распарсить filename, в котором сериализованы данные о картинке.
   * @param filename filename картинки.
   * @return Экземпляр MImg, содержащий все данные из filename'а.
   */
  override def apply(filename: String): MImg = {
    (new Parsers)
      .fromFileName(filename)
      .get
  }

  def apply(imgInfo: MImgInfoT): MImg = {
    apply(imgInfo.filename)
  }

  /**
   * Стереть отовсюду все картинки, которые расположены во всех известных моделях и имеющих указанный id.
   * Т.е. будет удалён оригинал и вся производная нарезка картинок отовсюду.
   * После выполнения метода картинку уже нельзя восстановить.
   * @param rowKey картинок.
   * @return Фьючерс для синхронизации.
   */
  def deleteAllFor(rowKey: UUID): Future[_] = {
    // TODO Добавить поддержку чистки из MMedia сюда.
    val permDelFut = MUserImg2.deleteById(rowKey)
    val permMetaDelFut = MUserImgMeta2.deleteById(rowKey)
    MLocalImg.deleteAllFor(rowKey) flatMap { _ =>
      permDelFut flatMap { _ =>
        permMetaDelFut
      }
    }
  }

  override def fromImg(img: MAnyImgT, dynOps2: Option[List[ImOp]] = None): MImg = {
    apply(img.rowKey, dynOps2.getOrElse(img.dynImgOps))
  }

}


/**
 * Реализация модели [[MImgT]] для работы напрямую с кассандрой.
 * Это legacy-модель картинок, потом будет удалена вслед за кассандрой.
 * Новая модель работает через MMedia, а во всяких мутных хранилищах хранятся только всякая блобота.
 */
case class MImg(override val rowKey: UUID,
                override val dynImgOps: Seq[ImOp])
  extends MImgT
  with PlayLazyMacroLogsImpl
{

  override type MImg_t = MImg
  override def thisT = this
  override def toWrappedImg = this

  override lazy val rowKeyStr = super.rowKeyStr

  override def withDynOps(dynImgOps2: Seq[ImOp]): MImg_t = {
    copy(dynImgOps = dynImgOps2)
  }

  override lazy val cropOpt = super.cropOpt

  override protected def _getImgMeta: Future[Option[IImgMeta]] = {
    MUserImgMeta2.getById(rowKey, qOpt)
  }

  override protected def _getImgBytes2: Enumerator[Array[Byte]] = {
    val fut = MUserImg2.getById(rowKey, qOpt) flatMap {
      case Some(img2) =>
        Future successful Enumerator( img2.imgBytes )
      case None =>
        Future failed new NoSuchElementException("_getImgBytes2(): Image does not exists in storage: " + this)
    }
    Enumerator.flatten(fut)
  }

  override protected def _updateMetaWith(localWh: MImgSizeT, localImg: MLocalImgT): Unit = {
    for (rawImgMeta <- localImg.rawImgMeta) {
      val md0 = rawImgMeta.fold
        { Map.empty[String, String] }
        { v => ImgFormUtil.imgMeta2md(v) }
      val mdWh = ImgFormUtil.imgMeta2md(localWh)
      val md = if (md0.nonEmpty) md0 ++ mdWh else mdWh
      val q = MUserImg2.qOpt2q( qOpt )
      MUserImgMeta2(md, q, rowKey)
        .save
        .onFailure { case ex: Throwable =>
          LOGGER.warn(s"_updateMetaWith($localWh, $localImg) [$rowKey, $dynImgOps] Failed to save image wh to PERMANENT", ex)
        }
    }
  }

  /** Сохранена ли текущая картинка в постоянном хранилище?
    * Метод просто проверяет наличие любых записей с указанным ключом в cassandra-моделях. */
  override def existsInPermanent: Future[Boolean] = {
    // Наличие метаданных не проверяет, т.к. там проблемы какие-то с isExists().
    MUserImg2.isExists(rowKey, qOpt)
  }


  override protected def _doSaveToPermanent(loc: MLocalImgT): Future[_] = {
    // Собираем и сохраняем метаданные картинки:
    val q1 = MUserImg2.qOpt2q(qOpt)
    val metaSaveFut = loc.rawImgMeta flatMap { imdOpt =>
      val imd = imdOpt.get
      val mui2meta = MUserImgMeta2(
        id = rowKey,
        q = q1,
        md = ImgFormUtil.imgMeta2md( imd ),
        timestamp = imd.dateCreated
      )
      mui2meta.save
    }
    // Сохраняем содержимое картинки:
    val mui2 = MUserImg2(
      id  = rowKey,
      q   = q1,
      img = ByteBuffer.wrap(loc.imgBytes),
      timestamp = new DateTime(loc.timestampMs)
    )
    val mui2saveFut = mui2.save
    // Объединяем фьючерсы
    mui2saveFut flatMap { _ =>
      metaSaveFut
    }
  }

  /** Удаление текущей картинки отовсюду вообще. Родственные картинки (др.размеры/нарезки) не удаляются. */
  override def delete: Future[_] = {
    val _qOpt = qOpt
    // Удаляем из permanent-хранилища.
    val permDelFut = MUserImg2.deleteOne(rowKey, _qOpt)
    val permMetaDelFut = MUserImgMeta2.deleteOne(rowKey, _qOpt)
    // Удаляем с локалхоста и объединяем с остальными фьючерсами удаления.
    toLocalInstance.delete flatMap { _ =>
      permDelFut flatMap { _ =>
        permMetaDelFut
      }
    }
  }

}


