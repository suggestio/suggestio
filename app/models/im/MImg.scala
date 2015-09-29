package models.im

import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.util.UUID

import io.suggest.model.img.IImgMeta
import io.suggest.model.{MUserImgMeta2, MUserImg2}
import io.suggest.util.UuidUtil
import models._
import org.joda.time.DateTime
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.QueryStringBindable
import util.qsb.QsbSigner
import util.secure.SecretGetter
import util.xplay.CacheUtil
import util.{PlayMacroLogsI, PlayLazyMacroLogsImpl}
import play.api.Play.{current, configuration, isProd}
import util.img.{ImgFileNameParsers, ImgFormUtil}

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


object MImg extends PlayLazyMacroLogsImpl with ImgFileNameParsers { model =>

  import LOGGER._

  val ORIG_META_CACHE_SECONDS: Int = configuration.getInt("m.img.org.meta.cache.ttl.seconds") getOrElse 60

  val SIGN_SUF   = ".sig"
  val IMG_ID_SUF = ".id"


  /** Статический секретный ключ для подписывания запросов к dyn-картинкам. */
  private[models] val SIGN_SECRET: String = {
    val sg = new SecretGetter {
      override val confKey = "dynimg.sign.key"
      override def useRandomIfMissing = isProd
      override def LOGGER = model.LOGGER
    }
    sg()
  }

  /** Использовать QSB[UUID] напрямую нельзя, т.к. он выдает не-base64-выхлопы, что вызывает конфликты. */
  def rowKeyB(implicit strB: QueryStringBindable[String]) = {
    new QueryStringBindable[UUID] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, UUID]] = {
        strB.bind(key, params).map {
          _.right.flatMap { raw =>
            try {
              Right( UuidUtil.base64ToUuid(raw) )
            } catch {
              case ex: Exception =>
                val msg = "img id missing or invalid: "
                warn( s"bind($key) $msg: $raw" )
                Left(msg)
            }
          }
        }
      }

      override def unbind(key: String, value: UUID): String = {
        val uuid64 = UuidUtil.uuidToBase64(value)
        strB.unbind(key, uuid64)
      }
    }
  }

  def qsbStandalone = {
    import ImOp._
    import QueryStringBindable._
    qsb
  }

  /** routes-биндер для query-string. */
  implicit def qsb(implicit strB: QueryStringBindable[String],
                   imOpsOptB: QueryStringBindable[Option[Seq[ImOp]]] ): QueryStringBindable[MImg] = {
    new QueryStringBindable[MImg] {

      /** Создать подписывалку для qs. */
      def getQsbSigner(key: String) = new QsbSigner(SIGN_SECRET, s"$key$SIGN_SUF")

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MImg]] = {
        // Собираем результат
        val keyDotted = if (!key.isEmpty) s"$key." else key
        for {
          // TODO Надо бы возвращать invalid signature при ошибке, а не not found.
          params2         <- getQsbSigner(key).signedOrNone(keyDotted, params)
          maybeImgId      <- rowKeyB.bind(key + IMG_ID_SUF, params2)
          maybeImOpsOpt   <- imOpsOptB.bind(keyDotted, params2)
        } yield {
          maybeImgId.right.flatMap { imgId =>
            maybeImOpsOpt.right.map { imOpsOpt =>
              val imOps = imOpsOpt.getOrElse(Nil)
              MImg(imgId, imOps)
            }
          }
        }
      }

      override def unbind(key: String, value: MImg): String = {
        val imgIdRaw = rowKeyB.unbind(key + IMG_ID_SUF, value.rowKey)
        val imgOpsOpt = if (value.hasImgOps) Some(value.dynImgOps) else None
        val imOpsUnbinded = imOpsOptB.unbind(s"$key.", imgOpsOpt)
        val unsignedResult = if (imOpsUnbinded.isEmpty) {
          imgIdRaw
        } else {
          imgIdRaw + "&" + imOpsUnbinded
        }
        getQsbSigner(key).mkSigned(key, unsignedResult)
      }
    }
  }

  /** Парсер filename'ов, выдающий на выходе готовые экземпляры MImg. */
  def fileName2miP: Parser[MImg] = {
    fileNameP ^^ {
      case uuid ~ imOps  =>  MImg(uuid, imOps)
    }
  }

  /**
   * Распарсить filename, в котором сериализованы данные о картинке.
   * @param filename filename картинки.
   * @return Экземпляр MImg, содержащий все данные из filename'а.
   */
  def apply(filename: String): MImg = {
    parseAll(fileName2miP, filename).get
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

}


import MImg._


trait MImgT extends MAnyImgT with PlayLazyMacroLogsImpl {

  def rowKey: UUID
  def dynImgOps: Seq[ImOp]

  def thisT: MImg_t

  lazy val toLocalInstance = MLocalImg(rowKey, dynImgOps)

  lazy val rowKeyStr = UuidUtil.uuidToBase64(rowKey)

  override lazy val dynImgOpsString = super.dynImgOpsString

  def qOpt: Option[String] = {
    if (isOriginal) {
      None
    } else {
      Some( dynImgOpsString )
    }
  }

  protected def _thisToOriginal: MImg_t

  /** Дать экземпляр MImg на исходный немодифицированный оригинал. */
  lazy val original: MImg_t = {
    if (hasImgOps) {
      _thisToOriginal
    } else {
      thisT
    }
  }

  /** Есть ли операции в dynImgOps? */
  override def hasImgOps: Boolean = dynImgOps.nonEmpty

  /** Имя файла картинки. Испрользуется как сериализованное представление данных по картинке. */
  override lazy val fileName: String = super.fileName

  /** Прочитать картинку из реального хранилища в файл, если ещё не прочитана. */
  override lazy val toLocalImg: Future[Option[MLocalImg]] = {
    val inst = toLocalInstance
    if (inst.isExists) {
      inst.touchAsync()
      Future successful Some(inst)
    } else {
      _localImgBytes.map { bytesOpt =>
        bytesOpt.map { imgBytes =>
          inst.writeIntoFile(imgBytes)
          inst
        }
      }
    }
  }

  protected def _localImgBytes: Future[Option[Array[Byte]]]

  /** Закешированный результат чтения метаданных из постоянного хранилища. */
  lazy val permMetaCached: Future[Option[IImgMeta]] = {
    CacheUtil.getOrElse(fileName + ".giwh", ORIG_META_CACHE_SECONDS) {
      _getImgMeta
    }
  }

  protected def _getImgMeta: Future[Option[IImgMeta]]

  /**
   * Узнать параметры изображения, описываемого экземпляром этой модели.
   * @return Фьючерс с пиксельным размером картинки.
   */
  override lazy val getImageWH: Future[Option[ISize2di]] = {
    // Фетчим паралельно из обеих моделей. Кто первая, от той и принимаем данные.
    val mimg2Fut = permMetaCached
      .filter(_.isDefined)
    val localInst = toLocalInstance
    lazy val logPrefix = "getImageWh(" + fileName + "): "
    val fut = if (localInst.isExists) {
      // Есть локальная картинка. Попробовать заодно потанцевать вокруг неё.
      val localFut = localInst.getImageWH
      mimg2Fut.recoverWith {
        case ex: Exception =>
          if (!ex.isInstanceOf[NoSuchElementException])
            LOGGER.warn(logPrefix + "Unable to read img info from PERMANENT models", ex)
          localFut
      }

    } else {
      // Сразу запускаем выкачивание локальной картинки. Если не понадобится сейчас, то скорее всего понадобится
      // чуть позже -- на раздаче самой картинки, а не её метаданных.
      val toLocalImgFut = toLocalImg
      mimg2Fut.recoverWith { case ex: Throwable =>
        // Запустить детектирование размеров.
        val whOptFut = toLocalImgFut.flatMap { localImgOpt =>
          localImgOpt.fold {
            LOGGER.warn(logPrefix + "local img was NOT read. cannot collect img meta.")
            Future successful Option.empty[MImgInfoMeta]
          } { _.getImageWH }
        }
        if (!ex.isInstanceOf[NoSuchElementException])
          LOGGER.debug(logPrefix + "No wh in DB, and nothing locally stored. Recollection img meta")
        // Сохранить полученные метаданные в хранилище.
        // Если есть уже сохраненная карта метаданных, то дополнить их данными WH, а не перезатереть.
        for (localWhOpt <- whOptFut;  localImgOpt <- toLocalImgFut) {
          for (localWh <- localWhOpt;  localImg <- localImgOpt) {
            _updateMetaWith(localWh, localImg)
          }
        }
        // Вернуть фьючерс с метаданными, не дожидаясь сохранения оных.
        whOptFut
      }
    }
    // Любое исключение тут можно подавить:
    fut.recover {
      case ex: Exception =>
        LOGGER.warn(logPrefix + "Unable to read img info meta from all models", ex)
        None
    }
  }

  /** Потенциально ненужная операция обновления метаданных. В новой архитектуре её быть не должно бы,
    * т.е. метаданные обязательные изначально. */
  protected def _updateMetaWith(localWh: MImgSizeT, localImg: MLocalImgT): Unit

  /** Отправить лежащее в файле на диске в постоянное хранилище. */
  def saveToPermanent: Future[_] = {
    val loc = toLocalInstance
    if (loc.isExists) {
      _doSaveToPermanent(loc: MLocalImg)
    } else {
      Future failed new FileNotFoundException("Img file not exists localy - unable to save into permanent storage: " + loc.file.getAbsolutePath)
    }
  }

  protected def _doSaveToPermanent(loc: MLocalImgT): Future[_]


  override lazy val rawImgMeta: Future[Option[IImgMeta]] = {
    permMetaCached
      .filter(_.isDefined)
      .recoverWith {
        // Пытаемся прочитать эти метаданные из модели MLocalImg.
        case ex: Exception  =>  toLocalInstance.rawImgMeta
      }
  }

}


/** Реализация модели [[MImgT]] для работы напрямую с кассандрой.
  * Это legacy-модель картинок, потом будет удалена вслед за кассандрой.
  * Новая модель работает через MMedia, а во всяких мутных хранилищах хранятся только всякая блобота. */
case class MImg(override val rowKey: UUID,
                override val dynImgOps: Seq[ImOp])
  extends MImgT
  with PlayLazyMacroLogsImpl
{

  override type MImg_t = MImg
  override def thisT = this
  override def toWrappedImg = this
  override protected def _thisToOriginal = copy(dynImgOps = Nil)

  override lazy val cropOpt = super.cropOpt

  override protected def _getImgMeta: Future[Option[IImgMeta]] = {
    MUserImgMeta2.getById(rowKey, qOpt)
  }

  override protected def _localImgBytes: Future[Option[Array[Byte]]] = {
    for (img2Opt <- MUserImg2.getById(rowKey, qOpt)) yield {
      img2Opt.map(_.imgBytes)
    }
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
  def existsInPermanent: Future[Boolean] = {
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
}


/** Интерфейс, объединяющий MImg и MLocalImg. */
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

