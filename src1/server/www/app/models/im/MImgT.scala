package models.im

import io.suggest.common.qs.QsConstants
import io.suggest.compress.MCompressAlgo
import io.suggest.img.MImgFormat
import io.suggest.n2.media.storage.MStorage
import io.suggest.primo.TypeT
import io.suggest.sec.QsbSigner
import io.suggest.sec.m.SecretKeyInit
import io.suggest.util.UuidUtil
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.xplay.qsb.AbstractQueryStringBindable
import io.suggest.url.bind.QueryStringBindableUtil._
import play.api.mvc.QueryStringBindable

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

object MImgT extends MacroLogsImpl with SecretKeyInit { model =>

  def SIGN_FN             = "sig"
  def IMG_ID_FN           = "id"
  def DYN_FORMAT_FN       = "df"
  def COMPRESS_ALGO_FN    = "ca"

  override def CONF_KEY = "dynimg.sign.key"
  private var SIGN_SECRET: String = _
  override def setSignSecret(secretKey: String): Unit = {
    SIGN_SECRET = secretKey
  }

  /** Использовать QSB[UUID] напрямую нельзя, т.к. он выдает не-base64-выхлопы, что вызывает конфликты. */
  def rowKeyB(implicit strB: QueryStringBindable[String]): QueryStringBindable[String] = {
    new AbstractQueryStringBindable[String] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, String]] = {
        for (rawEith <- strB.bind(key, params)) yield {
          rawEith.flatMap { raw =>
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
                        imgFormatOptB     : QueryStringBindable[Option[MImgFormat]],
                        imOpsOptB         : QueryStringBindable[Option[Seq[ImOp]]],
                        compressAlgoOptB  : QueryStringBindable[Option[MCompressAlgo]]
                       ): QueryStringBindable[MImgT] = {
    new AbstractQueryStringBindable[MImgT] {

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
          imgFormatOptE       <- imgFormatOptB.bind(k(DYN_FORMAT_FN), params2)
          imOpsOptE           <- imOpsOptB.bind(keyDotted, params2)
          compressAlgoOptE    <- compressAlgoOptB.bind(k(COMPRESS_ALGO_FN), params2)
        } yield {
          for {
            imgId             <- nodeIdE
            imgFormatOpt      <- imgFormatOptE
            imOpsOpt          <- imOpsOptE
            compressAlgoOpt   <- compressAlgoOptE
          } yield {
            val imOps = imOpsOpt getOrElse Nil
            val dynImgId = MDynImgId(
              origNodeId    = imgId,
              imgFormat     = imgFormatOpt,
              imgOps        = imOps,
              compressAlgo  = compressAlgoOpt,
            )
            MImg3( dynImgId )
          }
        }
      }

      override def unbind(key: String, value: MImgT): String = {
        val k = key1F(key)

        val unsignedRes = _mergeUnbinded1(
          rowKeyB.unbind            ( k(IMG_ID_FN),         value.dynImgId.origNodeId),
          imgFormatOptB.unbind      ( k(DYN_FORMAT_FN),     value.dynImgId.imgFormat),
          imOpsOptB.unbind          ( s"$key${QsConstants.KEY_PARTS_DELIM_STR}",    Option.when(value.dynImgId.hasImgOps)(value.dynImgId.imgOps) ),
          compressAlgoOptB.unbind   (k(COMPRESS_ALGO_FN),   value.dynImgId.compressAlgo),
        )

        getQsbSigner(key)
          .mkSigned(key, unsignedRes)
      }
    }
  }

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

