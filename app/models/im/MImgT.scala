package models.im

import java.io.FileNotFoundException
import java.util.UUID

import io.suggest.di.ICacheApiUtil
import io.suggest.itee.IteeUtil
import io.suggest.model.n2.media.IMMedias
import io.suggest.model.n2.media.storage.MStorage
import io.suggest.primo.TypeT
import io.suggest.util.UuidUtil
import models.{IImgMeta, _}
import models.mproj.IMCommonDi
import play.api.mvc.QueryStringBindable
import util.qsb.QsbSigner
import util.secure.SecretGetter
import util.{PlayMacroLogsI, PlayMacroLogsImpl}

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.09.15 12:26
  * Description: Файл с трейтами для сборки конкретных моделей permanent-хранения картинок.
  * Появилась когда-то давно и внезапно, но формировалась и росла в ходе больших реформ:
  * - переезда картинок на n2+MMedia (Модель внедрилась по всему проекту вместо MImg, в т.ч. routes).
  * - Немного во времена cassandra -> seaweedfs.
  *
  * Основная цель существования модели: абстрагировать MImg3 или иную конкретную реализацию
  * модели permanent-хранилища от проекта, чтобы облегчить возможный "переезд" конкретной модели.
  *
  * Не знаю, насколько это всё актуально в августе 2016, но модель пока здесь.
  */

object MImgT extends PlayMacroLogsImpl { model =>

  import play.api.Play.{current, isProd}

  val SIGN_SUF   = ".sig"
  val IMG_ID_SUF = ".id"

  /** Использовать QSB[UUID] напрямую нельзя, т.к. он выдает не-base64-выхлопы, что вызывает конфликты. */
  def rowKeyB(implicit strB: QueryStringBindable[String]): QueryStringBindable[String] = {
    new QueryStringBindable[String] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, String]] = {
        for (rawEith <- strB.bind(key, params)) yield {
          rawEith.right.flatMap { raw =>
            try {
              // TODO Может спилить отсюда проверку эту?
              UuidUtil.base64ToUuid(raw)
              Right(raw)
            } catch {
              case ex: Exception =>
                val msg = "img id missing or invalid: "
                LOGGER.warn( s"bind($key) $msg: $raw" )
                Left(msg)
            }
          }
        }
      }

      override def unbind(key: String, value: String): String = {
        strB.unbind(key, value)
      }
    }
  }

  /** Статический секретный ключ для подписывания запросов к dyn-картинкам. */
  private val SIGN_SECRET: String = {
    val sg = new SecretGetter {
      override val confKey = "dynimg.sign.key"
      override def useRandomIfMissing = isProd
      override def LOGGER = model.LOGGER
    }
    sg()
  }

  def qsbStandalone = {
    import ImOp._
    import QueryStringBindable._
    qsb
  }

  /** routes-биндер для query-string. */
  implicit def qsb(implicit
                   strB: QueryStringBindable[String],
                   imOpsOptB: QueryStringBindable[Option[Seq[ImOp]]]
                  ): QueryStringBindable[MImgT] = {
    new QueryStringBindable[MImgT] {

      /** Создать подписывалку для qs. */
      def getQsbSigner(key: String) = new QsbSigner(SIGN_SECRET, s"$key$SIGN_SUF")

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MImgT]] = {
        // Собираем результат
        val keyDotted = if (!key.isEmpty) s"$key." else key
        for {
          // TODO Надо бы возвращать invalid signature при ошибке, а не not found.
          params2         <- getQsbSigner(key).signedOrNone(keyDotted, params)
          maybeImgId      <- rowKeyB.bind(key + IMG_ID_SUF, params2)
          maybeImOpsOpt   <- imOpsOptB.bind(keyDotted, params2)
        } yield {
          for {
            imgId     <- maybeImgId.right
            imOpsOpt  <- maybeImOpsOpt.right
          } yield {
            val imOps = imOpsOpt.getOrElse(Nil)
            MImg3(imgId, imOps)
          }
        }
      }

      override def unbind(key: String, value: MImgT): String = {
        val imgIdRaw = rowKeyB.unbind(key + IMG_ID_SUF, value.rowKeyStr)
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

}


/** Частичная реализация [[MAnyImgsT]] для permanent-моделей.
  * Появилась при окончательном DI-рефакторинге Img-моделей для упрощение переезда
  * кода из MImgT в статику. */
trait MImgsT
  extends MAnyImgsT[MImgT]
    with PlayMacroLogsI
    with ICacheApiUtil
    with IMMedias
    with IMCommonDi
    with IMLocalImgs
{

  import mCommonDi._

  protected def _mediaOptFut(mimg: MImgT): Future[Option[MMedia]] = {
    mMedias.getById(mimg._mediaId)
  }
  protected def _mediaFut(mediaOptFut: Future[Option[MMedia]]): Future[MMedia] = {
    mediaOptFut.map(_.get)
  }

  override def toLocalImg(mimg: MImgT): Future[Option[MLocalImg]] = {
    val inst = mimg.toLocalInstance
    if (mLocalImgs.isExists(inst)) {
      mLocalImgs.touchAsync( inst )
      Future.successful( Some(inst) )
    } else {
      // Защищаемся от параллельных чтений одной и той же картинки. Это может создать ненужную нагрузку на сеть.
      cacheApiUtil.getOrElseFut(mimg.fileName + ".2LOC", 4.seconds) {
        // Запускаем поточное чтение из модели.
        val enumer = getStream(mimg)

        // Подготовится к запуску записи в файл.
        mLocalImgs.prepareWriteFile( inst )

        // Запустить запись в файл.
        val toFile = mLocalImgs.fileOf(inst)
        val writeFut = for {
          _ <- IteeUtil.writeIntoFile(enumer, toFile)
        } yield {
          Option(inst)
        }

        // Отработать ошибки записи.
        writeFut.recover { case ex: Throwable =>
          val logPrefix = "toLocalImg(): "
          if (ex.isInstanceOf[NoSuchElementException]) {
            if (LOGGER.underlying.isDebugEnabled) {
              if (mimg.isOriginal)
                LOGGER.debug(s"$logPrefix img not found in permanent storage: $toFile", ex)
              else
                LOGGER.debug(s"$logPrefix non-orig img not in permanent storage: $toFile")
            }
          } else {
            LOGGER.warn(s"$logPrefix _getImgBytes2 or writeIntoFile $toFile failed", ex)
          }
          None
        }
      }
    }
  }

  val ORIG_META_CACHE_SECONDS: Int = configuration.getInt("m.img.org.meta.cache.ttl.seconds")
    .getOrElse(60)

  /** Закешированный результат чтения метаданных из постоянного хранилища. */
  def permMetaCached(mimg: MImgT): Future[Option[IImgMeta]] = {
    cacheApiUtil.getOrElseFut(mimg.fileName + ".giwh", ORIG_META_CACHE_SECONDS.seconds) {
      _getImgMeta(mimg)
    }
  }

  protected def _getImgMeta(mimg: MImgT): Future[Option[IImgMeta]]

  /** Получить ширину и длину картинки. */
  override def getImageWH(mimg: MImgT): Future[Option[ISize2di]] = {
    // Фетчим паралельно из обеих моделей. Кто первая, от той и принимаем данные.
    val mimg2Fut = permMetaCached(mimg)
      .filter(_.isDefined)
    val localInst = mimg.toLocalInstance
    lazy val logPrefix = "getImageWh(" + mimg.fileName + "): "
    val fut = if (mLocalImgs.isExists(localInst)) {
      // Есть локальная картинка. Попробовать заодно потанцевать вокруг неё.
      val localFut = mLocalImgs.getImageWH(localInst)
      mimg2Fut.recoverWith {
        case ex: Exception =>
          if (!ex.isInstanceOf[NoSuchElementException])
            LOGGER.warn(logPrefix + "Unable to read img info from PERMANENT models", ex)
          localFut
      }

    } else {
      // Сразу запускаем выкачивание локальной картинки. Если не понадобится сейчас, то скорее всего понадобится
      // чуть позже -- на раздаче самой картинки, а не её метаданных.
      val toLocalImgFut = toLocalImg(mimg)
      mimg2Fut.recoverWith { case ex: Throwable =>
        // Запустить детектирование размеров.
        val whOptFut = toLocalImgFut.flatMap { localImgOpt =>
          localImgOpt.fold {
            LOGGER.warn(logPrefix + "local img was NOT read. cannot collect img meta.")
            Future.successful( Option.empty[MImgInfoMeta] )
          } { mLocalImgs.getImageWH }
        }
        if (ex.isInstanceOf[NoSuchElementException])
          LOGGER.debug(logPrefix + "No wh in DB, and nothing locally stored. Recollection img meta")
        // Сохранить полученные метаданные в хранилище.
        // Если есть уже сохраненная карта метаданных, то дополнить их данными WH, а не перезатереть.
        for (localWhOpt <- whOptFut;  localImgOpt <- toLocalImgFut) {
          for (localWh <- localWhOpt;  localImg <- localImgOpt) {
            _updateMetaWith(mimg, localWh, localImg)
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
  protected def _updateMetaWith(mimg: MImgT, localWh: MImgSizeT, localImg: MLocalImg): Unit

  override def rawImgMeta(mimg: MImgT): Future[Option[IImgMeta]] = {
    permMetaCached(mimg)
      .filter(_.isDefined)
      .recoverWith {
        // Пытаемся прочитать эти метаданные из модели MLocalImg.
        case ex: Exception  =>
          mLocalImgs.rawImgMeta( mimg.toLocalInstance )
      }
  }

  /** Отправить лежащее в файле на диске в постоянное хранилище. */
  def saveToPermanent(mimg: MImgT): Future[_] = {
    val loc = mimg.toLocalInstance
    if (mLocalImgs.isExists(loc)) {
      _doSaveToPermanent(mimg)
    } else {
      val ex = new FileNotFoundException(s"saveToPermanent($mimg): Img file not exists localy - unable to save into permanent storage: ${mLocalImgs.fileOf(loc).getAbsolutePath}")
      Future.failed(ex)
    }
  }

  protected def _doSaveToPermanent(mimg: MImgT): Future[_]

  /** Существует ли картинка в хранилище? */
  def existsInPermanent(mimg: MImgT): Future[Boolean]

}


/** Абстрактная модель MImg. В изначальной задумке её не было, но пришлось переезжать
  * на N2 с MMedia, сохраняя совместимость, поэтому MImg слегка разделилась на куски. */
abstract class MImgT extends MAnyImgT {

  def rowKey: UUID
  def dynImgOps: Seq[ImOp]

  /** id в рамках модели MMedia. */
  def _mediaId: String

  def thisT: MImg_t

  lazy val toLocalInstance = MLocalImg(rowKey, dynImgOps)

  def rowKeyStr = UuidUtil.uuidToBase64(rowKey)

  /** Используемое медиа-хранилище для данного элемента модели permanent img. */
  def storage: MStorage

  /** Пользовательское имя файла, если известно. */
  def userFileName: Option[String]

  override lazy val dynImgOpsString = super.dynImgOpsString

  def qOpt: Option[String] = {
    if (isOriginal) {
      None
    } else {
      Some( dynImgOpsString )
    }
  }

  protected def _thisToOriginal: MImg_t = withDynOps(Nil)

  def withDynOps(dynImgOps2: Seq[ImOp]): MImg_t

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

}


/** Интерфейс для объектов-компаньонов, умеющих собирать инстансы MImg* моделей из filename. */
trait IMImgCompanion extends TypeT {
  override type T <: MImgT
  def apply(fileName: String): T
  def fromImg(img: MAnyImgT, dynOps2: Option[List[ImOp]] = None): T
}

