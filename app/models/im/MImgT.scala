package models.im

import java.io.FileNotFoundException
import java.util.UUID

import io.suggest.itee.IteeUtil
import io.suggest.model.img.IImgMeta
import io.suggest.primo.TypeT
import io.suggest.util.UuidUtil
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumerator
import play.api.mvc.QueryStringBindable
import play.api.Play.{current, isProd, configuration}
import util.qsb.QsbSigner
import util.secure.SecretGetter
import util.xplay.CacheUtil
import util.PlayMacroLogsImpl

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 12:26
 * Description: Абстрактная модель дескрипторов картинок в нелокальных хранилищах.
 */
object MImgT extends PlayMacroLogsImpl { model =>

  val ORIG_META_CACHE_SECONDS: Int = configuration.getInt("m.img.org.meta.cache.ttl.seconds") getOrElse 60

  val SIGN_SUF   = ".sig"
  val IMG_ID_SUF = ".id"

  private val mImg3 = current.injector.instanceOf[MImgs3]

  /** Использовать QSB[UUID] напрямую нельзя, т.к. он выдает не-base64-выхлопы, что вызывает конфликты. */
  def rowKeyB(implicit strB: QueryStringBindable[String]): QueryStringBindable[String] = {
    new QueryStringBindable[String] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, String]] = {
        strB.bind(key, params).map {
          _.right.flatMap { raw =>
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
            mImg3(imgId, imOps)
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


/** Абстрактная модель MImg. В изначальной задумке её не было, но пришлось переезжать
  * на N2 с MMedia, сохраняя совместимость, поэтому MImg слегка разделилась на куски. */
abstract class MImgT extends MAnyImgT {

  def rowKey: UUID
  def dynImgOps: Seq[ImOp]

  def thisT: MImg_t

  lazy val toLocalInstance = MLocalImg(rowKey, dynImgOps)

  def rowKeyStr = UuidUtil.uuidToBase64(rowKey)

  override lazy val dynImgOpsString = super.dynImgOpsString

  /** Существует ли картинка в хранилище? */
  def existsInPermanent: Future[Boolean]

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

  /** Прочитать картинку из локального хранилища в файл, если ещё не прочитана. */
  override lazy val toLocalImg: Future[Option[MLocalImg]] = {
    val inst = toLocalInstance
    if (inst.isExists) {
      inst.touchAsync()
      Future successful Some(inst)
    } else {
      val enumer = _getImgBytes2
      inst.prepareWriteFile()
      IteeUtil.writeIntoFile(enumer, inst.file)
        .map { _ => Option(inst) }
        .recover { case ex: Throwable =>
          if (ex.isInstanceOf[NoSuchElementException]) {
            if (LOGGER.underlying.isDebugEnabled) {
              if (isOriginal)
                LOGGER.debug("toLocalImg: img not found in permanent storage: " + inst.file, ex)
              else
                LOGGER.debug("toLocalImg: non-orig img not in permanent storage: " + inst.file)
            }
          } else {
            LOGGER.warn(s"toLocalImg: _getImgBytes2 or writeIntoFile ${inst.file} failed", ex)
          }
          None
        }
    }
  }

  /** Запустить чтение картинки из хранилища, получив на руки Enumerator сырых данных. */
  protected def _getImgBytes2: Enumerator[Array[Byte]]

  /** Закешированный результат чтения метаданных из постоянного хранилища. */
  lazy val permMetaCached: Future[Option[IImgMeta]] = {
    CacheUtil.getOrElse(fileName + ".giwh", MImgT.ORIG_META_CACHE_SECONDS) {
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
        if (ex.isInstanceOf[NoSuchElementException])
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


/** Интерфейс для объектов-компаньонов, умеющих собирать инстансы MImg* моделей из filename. */
trait IMImgCompanion extends TypeT {
  override type T <: MImgT
  def apply(fileName: String): T
  def fromImg(img: MAnyImgT, dynOps2: Option[List[ImOp]] = None): T
}
