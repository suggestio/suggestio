package models.im

import java.io.FileNotFoundException

import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.compress.MCompressAlgo
import io.suggest.di.ICacheApiUtil
import io.suggest.img.MImgFmt
import io.suggest.model.img.ImgSzDated
import io.suggest.model.n2.media.{IMMedias, MMedia}
import io.suggest.model.n2.media.storage.MStorage
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.primo.TypeT
import io.suggest.sec.QsbSigner
import io.suggest.sec.m.SecretGetter
import io.suggest.streams.StreamsUtil
import io.suggest.util.UuidUtil
import io.suggest.util.logs.{IMacroLogs, MacroLogsImpl}
import models.mproj.IMCommonDi
import play.api.mvc.QueryStringBindable

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

object MImgT extends MacroLogsImpl { model =>

  def SIGN_FN             = "sig"
  def IMG_ID_FN           = "id"
  def DYN_FORMAT_FN       = "df"
  def COMPRESS_ALGO_FN    = "ca"

  /** Использовать QSB[UUID] напрямую нельзя, т.к. он выдает не-base64-выхлопы, что вызывает конфликты. */
  def rowKeyB(implicit strB: QueryStringBindable[String]): QueryStringBindable[String] = {
    new QueryStringBindableImpl[String] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, String]] = {
        for (rawEith <- strB.bind(key, params)) yield {
          rawEith.right.flatMap { raw =>
            try {
              // TODO Может спилить отсюда проверку эту?
              UuidUtil.base64ToUuid(raw)
              Right(raw)
            } catch {
              case ex: Exception =>
                val msg = "img id missing or invalid : "
                LOGGER.warn( s"bind($key) $msg: $raw (${ex.getClass.getSimpleName})" )
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
      override def confKey = "dynimg.sign.key"
      override def LOGGER = model.LOGGER
    }
    sg()
  }

  def qsbStandalone = {
    import ImOp._
    import QueryStringBindable._
    import io.suggest.img.MImgFmtJvm._
    import io.suggest.compress.MCompressAlgosJvm._
    mImgTQsb
  }

  // TODO Унести это в MDynImgId!
  /** routes-биндер для query-string. */
  implicit def mImgTQsb(implicit
                        strB              : QueryStringBindable[String],
                        imgFmtB           : QueryStringBindable[MImgFmt],
                        imOpsOptB         : QueryStringBindable[Option[Seq[ImOp]]],
                        compressAlgoOptB  : QueryStringBindable[Option[MCompressAlgo]]
                       ): QueryStringBindable[MImgT] = {
    new QueryStringBindableImpl[MImgT] {

      /** Создать подписывалку для qs. */
      def getQsbSigner(key: String) = new QsbSigner(SIGN_SECRET, key1(key, SIGN_FN))

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MImgT]] = {
        // Собираем результат
        val k = key1F(key)
        val keyDotted = k("")
        for {
          // TODO Надо бы возвращать invalid signature при ошибке, а не not found.
          params2             <- getQsbSigner(key)
            .signedOrNone(keyDotted, params)
          nodeIdE             <- rowKeyB.bind(k(IMG_ID_FN), params2)
          dynFormatE          <- imgFmtB.bind(k(DYN_FORMAT_FN), params2)
          imOpsOptE           <- imOpsOptB.bind(keyDotted, params2)
          compressAlgoOptE    <- compressAlgoOptB.bind(k(COMPRESS_ALGO_FN), params2)
        } yield {
          for {
            imgId             <- nodeIdE.right
            dynFormat         <- dynFormatE.right
            imOpsOpt          <- imOpsOptE.right
            compressAlgoOpt   <- compressAlgoOptE.right
          } yield {
            val imOps = imOpsOpt.getOrElse(Nil)
            val dynImgId = MDynImgId(
              rowKeyStr     = imgId,
              dynFormat     = dynFormat,
              dynImgOps     = imOps,
              compressAlgo  = compressAlgoOpt
            )
            MImg3( dynImgId )
          }
        }
      }

      override def unbind(key: String, value: MImgT): String = {
        val k = key1F(key)
        val unsignedRes = _mergeUnbinded1(
          rowKeyB.unbind  (k(IMG_ID_FN),      value.dynImgId.rowKeyStr),
          imgFmtB.unbind  (k(DYN_FORMAT_FN),  value.dynImgId.dynFormat),
          imOpsOptB.unbind(s"$key.",          if (value.dynImgId.hasImgOps) Some(value.dynImgId.dynImgOps) else None),
          compressAlgoOptB.unbind(k(COMPRESS_ALGO_FN), value.dynImgId.compressAlgo)
        )
        getQsbSigner(key)
          .mkSigned(key, unsignedRes)
      }
    }
  }

}


/** Частичная реализация [[MAnyImgsT]] для permanent-моделей.
  * Появилась при окончательном DI-рефакторинге Img-моделей для упрощение переезда
  * кода из MImgT в статику. */
trait MImgsT
  extends MAnyImgsT[MImgT]
    with IMacroLogs
    with ICacheApiUtil
    with IMMedias
    with IMCommonDi
    with IMLocalImgs
{

  import mCommonDi._

  protected val streamsUtil: StreamsUtil


  def mediaOptFut(mimg: MImgT): Future[Option[MMedia]] = {
    mMedias.getById(mimg.dynImgId.mediaId)
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
      cacheApiUtil.getOrElseFut(mimg.dynImgId.fileName + ".2LOC", 4.seconds) {
        // Запускаем поточное чтение из модели.
        val source = getStream(mimg)

        // Подготовится к запуску записи в файл.
        mLocalImgs.prepareWriteFile( inst )

        // Запустить запись в файл.
        val toFile = mLocalImgs.fileOf(inst)
        val writeFut = for {
          _ <- streamsUtil.sourceIntoFile(source, toFile)
        } yield {
          Option(inst)
        }

        // Отработать ошибки записи.
        writeFut.recover { case ex: Throwable =>
          val logPrefix = "toLocalImg(): "
          if (ex.isInstanceOf[NoSuchElementException]) {
            if (LOGGER.underlying.isDebugEnabled) {
              if (mimg.dynImgId.hasImgOps)
                LOGGER.debug(s"$logPrefix non-orig img not in permanent storage: $toFile")
              else
                LOGGER.debug(s"$logPrefix img not found in permanent storage: $toFile", ex)
            }
          } else {
            LOGGER.warn(s"$logPrefix _getImgBytes2 or writeIntoFile $toFile failed", ex)
          }
          None
        }
      }
    }
  }

  val ORIG_META_CACHE_SECONDS: Int = configuration.getOptional[Int]("m.img.org.meta.cache.ttl.seconds")
    .getOrElse(60)

  /** Закешированный результат чтения метаданных из постоянного хранилища. */
  def permMetaCached(mimg: MImgT): Future[Option[ImgSzDated]] = {
    cacheApiUtil.getOrElseFut(mimg.dynImgId.fileName + ".giwh", ORIG_META_CACHE_SECONDS.seconds) {
      _getImgMeta(mimg)
    }
  }

  protected def _getImgMeta(mimg: MImgT): Future[Option[ImgSzDated]]

  /** Получить ширину и длину картинки. */
  override def getImageWH(mimg: MImgT): Future[Option[ISize2di]] = {
    // Фетчим паралельно из обеих моделей. Кто первая, от той и принимаем данные.
    val mimg2Fut = permMetaCached(mimg)
      .filter(_.isDefined)

    val localInst = mimg.toLocalInstance
    lazy val logPrefix = s"getImageWh(${mimg.dynImgId.fileName}): "

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
            Future.successful( Option.empty[MSize2di] )
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
  protected def _updateMetaWith(mimg: MImgT, localWh: ISize2di, localImg: MLocalImg): Unit

  override def rawImgMeta(mimg: MImgT): Future[Option[ImgSzDated]] = {
    permMetaCached(mimg)
      .filter(_.isDefined)
      .recoverWith {
        // Пытаемся прочитать эти метаданные из модели MLocalImg.
        case _: Exception  =>
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

  override def withDynImgId(dynImgId: MDynImgId): MImgT

  /** id в рамках модели MMedia. */
  def mediaId: String

  def thisT: MImg_t

  lazy val toLocalInstance = MLocalImg(dynImgId)

  /** Используемое медиа-хранилище для данного элемента модели permanent img. */
  def storage: MStorage

  /** Пользовательское имя файла, если известно. */
  def userFileName: Option[String]

  def withDynOps(dynImgOps2: Seq[ImOp]): MImg_t

  /** Дать экземпляр MImg на исходный немодифицированный оригинал. */
  lazy val original: MImg_t = {
    if (dynImgId.hasImgOps) {
      withDynOps(Nil)
    } else {
      thisT
    }
  }

}


/** Интерфейс для объектов-компаньонов, умеющих собирать инстансы MImg* моделей из filename. */
trait IMImgCompanion extends TypeT {
  override type T <: MImgT
  def apply(fileName: String): T
  def fromImg(img: MAnyImgT, dynOps2: Option[List[ImOp]] = None): T
}

