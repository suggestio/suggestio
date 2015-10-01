package models.im

import java.util.UUID

import io.suggest.model.img.{ImgSzDated, IImgMeta}
import io.suggest.model.n2.media.storage.{CassandraStorage, IMediaStorage}
import io.suggest.model.n2.media.{MPictureMeta, MFileMeta}
import io.suggest.util.UuidUtil
import models._
import models.mfs.FileUtil
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumerator
import util.PlayLazyMacroLogsImpl
import util.event.SiowebNotifier.Implicts.sn
import util.img.ImgFileNameParsersImpl

import scala.concurrent.Future
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 17:27
 * Description: Реализация модели [[MImgT]] на базе MMedia, вместо прямого взаимодействия с кассандрой.
 */
object MImg3 {

  /** Реализация парсеров filename'ов в данную модель. */
  class Parsers extends ImgFileNameParsersImpl {

    override type T = MImg3

    override def fileName2miP: Parser[T] = {
      (uuidStrP ~ imOpsP) ^^ {
        case nodeId ~ dynImgOps =>
          apply(nodeId, dynImgOps)
      }
    }

  }

  def apply(fileName: String): MImg3 = {
    (new Parsers)
      .fromFileName(fileName)
      .get
  }

}


/** Трейт одного элемента модели [[MImg3]].
  * Появился из-за необходимости простого переключения между cassandra и seaweedfs. */
abstract class MImg3T extends MImgT {

  override type MImg_t <: MImg3T

  override lazy val rowKey: UUID = {
    UuidUtil.base64ToUuid(rowKeyStr)
  }

  lazy val _mediaId = MMedia.mkId(rowKeyStr, qOpt)

  // Не val потому что результат может меняться с None на Some() в результате сохранения картинки.
  def _mediaOptFut = MMedia.getById(_mediaId)
  def _mediaFut = _mediaOptFut.map(_.get)

  override protected lazy val _getImgMeta: Future[Option[IImgMeta]] = {
    _mediaOptFut map { mmediaOpt =>
      mmediaOpt.map { mmedia =>
        ImgSzDated(
          sz          = mmedia.picture.get,
          dateCreated = mmedia.file.dateCreated
        )
      }
    }
  }

  override def existsInPermanent: Future[Boolean] = {
    _mediaFut
      .flatMap { _.storage.isExist }
      .recover { case ex: Throwable =>
        if (!ex.isInstanceOf[NoSuchElementException])
          LOGGER.warn("isExist() or _mediaFut failed / " + this, ex)
        false
      }
  }

  override protected def _getImgBytes2: Enumerator[Array[Byte]] = {
    val fut = _mediaFut.map { mm =>
      mm.storage.read
    }
    Enumerator.flatten(fut)
  }

  /** Подготовить и вернуть новое медиа-хранилище для модели. */
  protected def _newMediaStorage: Future[IMediaStorage]

  /** Сохранить в постоянное хранилище, (пере-)создать MMedia. */
  override protected def _doSaveToPermanent(loc: MLocalImgT): Future[MMedia] = {
    val mli = toLocalInstance
    _mediaFut.recoverWith { case ex: Throwable =>
      // Перезаписывать нечего, т.к. элемент ещё не существует в MMedia.
      val whOptFut = mli.getImageWH
      val mimeFut = Future(mli.mime)
      val sha1Fut = Future(FileUtil.sha1(mli.file))
      val storFut = _newMediaStorage
      val szB = mli.file.length()
      for {
        whOpt <- whOptFut
        mime <- mimeFut
        sha1 <- sha1Fut
        stor <- storFut
        mm <- {
          val _mm = MMedia(
            nodeId = rowKeyStr,
            id = Some(_mediaId),
            file = MFileMeta(
              mime = mime,
              sizeB = szB,
              isOriginal = isOriginal,
              sha1 = Some(sha1)
            ),
            picture = whOpt.map(MPictureMeta.apply),
            storage = stor
          )
          _mm.save
            .map { _mmId => _mm }
        }
      } yield {
        mm
      }

    }.flatMap { mm =>
      mm.storage
        .write( mli.imgBytesEnumerator )
        .map { _ => mm }
    }
  }

  override protected def _updateMetaWith(localWh: MImgSizeT, localImg: MLocalImgT): Unit = {
    // should never happen
    // Необходимость апдейта метаданных возникает, когда обнаруживается, что нет метаднных.
    // В случае N2 MMedia, метаданные без блоба существовать не могут, и необходимость не должна наступать.
    LOGGER.warn(s"_updateMetaWith($localWh, $localImg) ignored and not implemented")
  }

  override def delete: Future[_] = {
    _mediaOptFut flatMap {
      case Some(mm) =>
        for {
          _ <- mm.storage.delete
          _ <- mm.delete
        } yield {
          true
        }
      case None =>
        Future successful false
    }
  }

}


case class MImg3(override val rowKeyStr: String,
                 override val dynImgOps: Seq[ImOp])
  extends MImg3T
  with PlayLazyMacroLogsImpl
  with I3Cassandra
{

  override type MImg_t = MImg3
  override def thisT: MImg_t = this
  override def toWrappedImg = this

  override def withDynOps(dynImgOps2: Seq[ImOp]): MImg3 = {
    copy(dynImgOps = dynImgOps2)
  }

}


/** Использовать кассандру для сохранения новых файлов. */
trait I3Cassandra extends MImg3T {

  override protected def _newMediaStorage: Future[IMediaStorage] = {
    val stor = CassandraStorage(rowKey, qOpt)
    Future successful stor
  }

}
