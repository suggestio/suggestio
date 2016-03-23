package models.im

import java.util.{NoSuchElementException, UUID}

import com.google.inject.{Inject, Singleton}
import io.suggest.fio.WriteRequest
import io.suggest.model.img.{IImgMeta, ImgSzDated}
import io.suggest.model.n2.media.storage.IMediaStorage
import io.suggest.model.n2.media.storage.swfs.SwfsStorages
import io.suggest.model.n2.media.{MFileMeta, MMedias, MPictureMeta}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.MBasicMeta
import io.suggest.util.UuidUtil
import models._
import models.mfs.FileUtil
import models.mproj.ICommonDi
import org.joda.time.DateTime
import play.api.libs.iteratee.Enumerator
import util.PlayLazyMacroLogsImpl
import util.img.ImgFileNameParsersImpl

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 17:27
 * Description: Реализация модели [[MImgT]] на базе MMedia, вместо прямого взаимодействия с кассандрой.
 * [[MImgs3]] -- DI-реализация объекта-компаньона.
 */
@Singleton
class MImgs3 @Inject()(
  val swfsStorages          : SwfsStorages,
  val mMedias               : MMedias,
  val mCommonDi             : ICommonDi
)
  extends IMImgCompanion
  with PlayLazyMacroLogsImpl
{

  override type T = MImg3

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

  override def apply(fileName: String): MImg3 = {
    val pr = (new Parsers).fromFileName(fileName)
    if (!pr.successful)
      LOGGER.error(s"""Failed to parse img from fileName <<$fileName>>:\n$pr""")
    pr.get
  }

  override def fromImg(img: MAnyImgT, dynOps2: Option[List[ImOp]] = None): MImg3 = {
    apply(img.rowKeyStr, dynOps2.getOrElse(img.dynImgOps))
  }

  /** Экстракция указателя на картинку из эджа узла N2.
    * @throws java.util.NoSuchElementException когда id узла-картинки не задан.
    */
  def apply(medge: IEdge): MImg3 = {
    val dops = {
      medge.info
        .dynImgArgs
        .fold( List.empty[ImOp] ) { imOpsStr =>
          val pr = (new Parsers).parseImgArgs(imOpsStr)
          pr.getOrElse {
            LOGGER.warn(s"apply($medge): Ignoring ops. Failed to parse imOps str '''$imOpsStr'''\n$pr")
            Nil
          }
      }
    }
    apply(medge.nodeIdOpt.get, dops)
  }

  def apply(rowKeyStr: String, dynImgOps: Seq[ImOp], userFileName: Option[String] = None): MImg3 = {
    MImg3(rowKeyStr, dynImgOps, this, userFileName)
  }

}


/** Трейт одного элемента модели [[MImg3]].
  * Появился из-за необходимости простого переключения между cassandra и seaweedfs. */
abstract class MImg3T extends MImgT {

  override type MImg_t <: MImg3T

  /** DI-инстанс статической части модели MMedia. */
  val companion: MImgs3

  import companion.mCommonDi._

  /** Пользовательское имя файла, если известно. */
  def userFileName: Option[String]

  def mMedias = companion.mMedias

  override lazy val rowKey: UUID = {
    UuidUtil.base64ToUuid(rowKeyStr)
  }

  lazy val _mediaId = mMedias.mkId(rowKeyStr, qOpt)

  // Не val потому что результат может меняться с None на Some() в результате сохранения картинки.
  def _mediaOptFut = mMedias.getById(_mediaId)
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
    val isExistsFut = _mediaFut
      .flatMap { _.storage.isExist }
    // возвращать false при ошибках.
    val resFut = isExistsFut.recover {
      // Если подавлять все ошибки связи, то система будет удалять все local imgs.
      case ex: NoSuchElementException =>
        false
    }
    // Залоггировать неожиданные экзепшены.
    isExistsFut.onFailure { case ex: Throwable =>
      if (!ex.isInstanceOf[NoSuchElementException])
        LOGGER.warn("isExist() or _mediaFut failed / " + this, ex)
    }
    resFut
  }

  override protected def _getImgBytes2: Enumerator[Array[Byte]] = {
    val enumFut = for {
      mm <- _mediaFut
      rr <- mm.storage.read()
    } yield {
      rr.data
    }
    Enumerator.flatten( enumFut )
  }

  /** Подготовить и вернуть новое медиа-хранилище для модели. */
  protected def _newMediaStorage: Future[IMediaStorage]

  /** Убедится, что в хранилищах существует сохраненный экземпляр MNode.
    * Если нет, то создрать и сохранить. */
  def ensureMnode(loc: MLocalImgT): Future[MNode] = {
    mNodeCache
      .getById(rowKeyStr)
      .map(_.get)
      .recoverWith { case ex: NoSuchElementException =>
        saveMnode(loc)
      }
  }

  /** Сгенерить новый экземпляр MNode и сохранить. */
  def saveMnode(loc: MLocalImgT): Future[MNode] = {
    val fnameFut = userFileName
      .fold [Future[String]] (loc.generateFileName) (Future.successful)
    val mnodeFut = for {
      perm    <- permMetaCached
      fname   <- fnameFut
    } yield {
      MNode(
        id = Some( rowKeyStr ),
        common = MNodeCommon(
          ntype         = MNodeTypes.Media.Image,
          isDependent   = true
        ),
        meta = MMeta(
          basic = MBasicMeta(
            techName    = Some(fname),
            dateCreated = perm.map(_.dateCreated)
              .getOrElse { DateTime.now() }
          )
        )
      )
    }
    // Запустить сохранение, вернуть экземпляр MNode.
    for {
      mnode <- mnodeFut
      _     <- mnode.save
    } yield {
      mnode
    }
  }

  /** Сохранить в постоянное хранилище, (пере-)создать MMedia. */
  override protected def _doSaveToPermanent(loc: MLocalImgT): Future[MMedia] = {
    val mimeFut = loc.mimeFut
    val media0Fut = _mediaFut

    media0Fut onSuccess { case res =>
      LOGGER.trace(s"_doSaveInPermanent($loc): MMedia already exist: $res")
    }

    // Вернуть экземпляр MMedia.
    val mediaFut: Future[MMedia] = media0Fut.recoverWith { case ex: Throwable =>
      // Перезаписывать нечего, т.к. элемент ещё не существует в MMedia.
      val whOptFut = loc.getImageWH
      val sha1Fut = Future( FileUtil.sha1(loc.file) )
      val storFut = _newMediaStorage

      if (!ex.isInstanceOf[NoSuchElementException])
        LOGGER.warn("_doSaveToPermanent(" + loc + "): _mediaFut() returned error, this = " + this, ex)

      val szB = loc.file.length()

      for {
        whOpt <- whOptFut
        mime  <- mimeFut
        sha1  <- sha1Fut
        stor  <- storFut
      } yield {
        MMedia(
          nodeId  = rowKeyStr,
          id      = Some(_mediaId),
          file    = MFileMeta(
            mime        = mime,
            sizeB       = szB,
            isOriginal  = isOriginal,
            sha1        = Some(sha1)
          ),
          picture = whOpt.map(MPictureMeta.apply),
          storage = stor,
          companion = mMedias
        )
      }
    }

    val mediaSavedFut = media0Fut.recoverWith { case ex: Throwable =>
      for {
        mmedia      <- mediaFut
        _mmediaId   <- mmedia.save
      } yield {
        mmedia
      }
    }

    // Параллельно запустить поиск и сохранение экземпляра MNode.
    val mnodeSaveFut = ensureMnode( loc )

    mnodeSaveFut onFailure { case ex: Throwable =>
      LOGGER.error("Failed to save picture MNode for local img " + loc)
    }

    // Параллельно выполнить заливку файла в постоянное надежное хранилище.
    val storWriteFut = for {
      mm   <- mediaFut
      mime <- mimeFut
      res  <- {
        val wargs = WriteRequest(
          contentType = mime,
          file        = loc.file
        )
        mm.storage
          .write( wargs )
      }
    } yield {
      res
    }

    storWriteFut onFailure { case ex: Throwable =>
      LOGGER.error("Failed to send to storage local image: " + loc)
    }

    // Дождаться завершения всех паралельных операций.
    for {
      _   <- mnodeSaveFut
      _   <- storWriteFut
      mm  <- mediaSavedFut
    } yield {
      mm
    }
  }

  override protected def _updateMetaWith(localWh: MImgSizeT, localImg: MLocalImgT): Unit = {
    // should never happen
    // Необходимость апдейта метаданных возникает, когда обнаруживается, что нет метаданных.
    // В случае N2 MMedia, метаданные без блоба существовать не могут, и необходимость не должна наступать.
    LOGGER.warn(s"_updateMetaWith($localWh, $localImg) ignored and not implemented")
  }

  override def delete: Future[_] = {
    _mediaOptFut.flatMap {
      case Some(mm) =>
        for {
          _ <- mm.storage.delete()
          _ <- mm.delete
        } yield {
          true
        }
      case None =>
        Future successful false
    }
  }

}


case class MImg3(
  override val rowKeyStr            : String,
  override val dynImgOps            : Seq[ImOp],
  override val companion            : MImgs3,   // TODO Использовать Assisted DI, и прямую инжекцию реальных зависимостей вместо инстанса компаньона.
  override val userFileName         : Option[String] = None
)
  extends MImg3T
  with PlayLazyMacroLogsImpl
  with I3SeaWeedFs
{

  override type MImg_t = MImg3
  override def thisT: MImg_t = this
  override def toWrappedImg = this

  override def withDynOps(dynImgOps2: Seq[ImOp]): MImg3 = {
    copy(dynImgOps = dynImgOps2)
  }

}


/** Использовать seaweedfs для сохранения новых картинок. */
trait I3SeaWeedFs extends MImg3T {

  override protected def _newMediaStorage = {
    companion.swfsStorages.assingNew()
  }
}
