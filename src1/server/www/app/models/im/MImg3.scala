package models.im

import java.time.OffsetDateTime
import java.util.{NoSuchElementException, UUID}
import javax.inject.{Inject, Singleton}

import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.suggest.async.StreamsUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.common.geom.d2.ISize2di
import io.suggest.fio.WriteRequest
import io.suggest.js.UploadConstants
import io.suggest.model.img.ImgSzDated
import io.suggest.model.n2.edge.MEdge
import io.suggest.model.n2.media.storage.{IMediaStorages, MStorages}
import io.suggest.model.n2.media.{MFileMeta, MMedia, MMedias, MPictureMeta}
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.playx.CacheApiUtil
import io.suggest.util.UuidUtil
import io.suggest.util.logs.{MacroLogsImpl, MacroLogsImplLazy}
import models.mfs.FileUtil
import models.mproj.ICommonDi
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
class MImgs3 @Inject() (
  val iMediaStorages        : IMediaStorages,
  val mMedias               : MMedias,
  val mNodes                : MNodes,
  fileUtil                  : FileUtil,
  override val streamsUtil  : StreamsUtil,
  override val cacheApiUtil : CacheApiUtil,
  override val mLocalImgs   : MLocalImgs,
  val mCommonDi             : ICommonDi
)
  extends MImgsT
  with MacroLogsImplLazy
{

  import mCommonDi._

  override def delete(mimg: MImgT): Future[_] = {
    _mediaOptFut(mimg).flatMap {
      case Some(mm) =>
        for {
          _ <- iMediaStorages.delete( mm.storage )
          _ <- mMedias.deleteById(mm.id.get)
        } yield {
          true
        }
      case None =>
        Future.successful(false)
    }
  }

  /** Выполнить стриминг данных картинки из SeaWeedFS. */
  override def getStream(mimg: MImgT): Source[ByteString, _] = {
    val srcFut = for {
      mm <- _mediaFut( _mediaOptFut(mimg) )
      rr <- iMediaStorages.read( mm.storage )
    } yield {
      rr.data
    }
    Source.fromFutureSource( srcFut )
  }

  override protected def _getImgMeta(mimg: MImgT): Future[Option[ImgSzDated]] = {
    for (mmediaOpt <- _mediaOptFut(mimg)) yield {
      for (mmedia <- mmediaOpt) yield {
        ImgSzDated(
          sz          = mmedia.picture.get,
          dateCreated = mmedia.file.dateCreated
        )
      }
    }
  }

  /** Потенциально ненужная операция обновления метаданных. В новой архитектуре её быть не должно бы,
    * т.е. метаданные обязательные изначально. */
  override protected def _updateMetaWith(mimg: MImgT, localWh: ISize2di, localImg: MLocalImg): Unit = {
    // should never happen
    // Необходимость апдейта метаданных возникает, когда обнаруживается, что нет метаданных.
    // В случае N2 MMedia, метаданные без блоба существовать не могут, и необходимость не должна наступать.
    LOGGER.warn(s"_updateMetaWith($localWh, $localImg) ignored and not implemented")
  }


  /** Убедится, что в хранилищах существует сохраненный экземпляр MNode.
    * Если нет, то создрать и сохранить. */
  def ensureMnode(mimg: MImgT): Future[MNode] = {
    mNodesCache
      .getById( mimg.rowKeyStr )
      .map(_.get)
      .recoverWith { case ex: NoSuchElementException =>
        saveMnode(mimg)
      }
  }

  /** Сгенерить новый экземпляр MNode и сохранить. */
  def saveMnode(mimg: MImgT): Future[MNode] = {
    val fnameFut = FutureUtil.opt2future(mimg.userFileName) {
      mLocalImgs.generateFileName( mimg.toLocalInstance )
    }
    val mnodeFut = for {
      perm    <- permMetaCached(mimg)
      fname   <- fnameFut
    } yield {
      // Собираем новый узел n2, когда все необходимые данные уже собраны...
      MNode(
        id = Some( mimg.rowKeyStr ),
        common = MNodeCommon(
          ntype         = MNodeTypes.Media.Image,
          isDependent   = true
        ),
        meta = MMeta(
          basic = MBasicMeta(
            techName    = Some(fname),
            dateCreated = perm.map(_.dateCreated)
              .getOrElse { OffsetDateTime.now() }
          )
        )
      )
    }

    // Запустить сохранение, вернуть экземпляр MNode.
    for {
      mnode <- mnodeFut
      _     <- mNodes.save(mnode)
    } yield {
      mnode
    }
  }

  override protected def _doSaveToPermanent(mimg: MImgT): Future[_] = {
    val loc = mimg.toLocalInstance
    val mimeFut = mLocalImgs.mimeFut(loc)
    val media0Fut = _mediaFut {
      _mediaOptFut( mimg )
    }

    lazy val logPrefix = s"_doSaveInPermanent($loc):"

    if (LOGGER.underlying.isTraceEnabled()) {
      for (res <- media0Fut)
        LOGGER.trace(s"$logPrefix MMedia already exist: $res")
    }

    val imgFile = mLocalImgs.fileOf(loc)
    // Вернуть экземпляр MMedia.
    val mediaFut: Future[MMedia] = media0Fut.recoverWith { case ex: Throwable =>
      // Перезаписывать нечего, т.к. элемент ещё не существует в MMedia.
      val whOptFut = mLocalImgs.getImageWH(loc)
      // TODO Допустить, что хэши уже просчитаны где-то в контроллере, не считать их тут...
      val hashesHexFut = fileUtil.mkHashesHexAsync(imgFile, UploadConstants.CleverUp.PICTURE_FILE_HASHES)
      val storFut = iMediaStorages.assignNew( mimg.storage )

      if (!ex.isInstanceOf[NoSuchElementException])
        LOGGER.warn(s"$logPrefix _mediaFut() returned error, mimg = $mimg", ex)

      val szB = imgFile.length()

      for {
        whOpt       <- whOptFut
        mime        <- mimeFut
        hashesHex   <- hashesHexFut
        stor        <- storFut
      } yield {
        MMedia(
          nodeId  = mimg.rowKeyStr,
          id      = Some( mimg._mediaId ),
          file    = MFileMeta(
            mime        = mime,
            sizeB       = szB,
            isOriginal  = mimg.isOriginal,
            hashesHex   = hashesHex
          ),
          // TODO Перепилить MPictureMeta, чтобы wh были просто полем-объектом, ИНДЕКСИРУЕМЫМ!
          picture = whOpt.map(MPictureMeta.apply),
          storage = stor
        )
      }
    }

    val mediaSavedFut = media0Fut.recoverWith { case _: Throwable =>
      for {
        mmedia      <- mediaFut
        _           <- mMedias.save(mmedia)
      } yield {
        mmedia
      }
    }

    // Параллельно запустить поиск и сохранение экземпляра MNode.
    val mnodeSaveFut = ensureMnode(mimg)

    for (_ <- mnodeSaveFut.failed)
      LOGGER.error(s"$logPrefix Failed to save picture MNode for local img " + loc)

    // Параллельно выполнить заливку файла в постоянное надежное хранилище.
    val storWriteFut = for {
      mm   <- mediaFut
      mime <- mimeFut
      res  <- {
        val wargs = WriteRequest(
          contentType = mime,
          file        = imgFile
        )
        iMediaStorages.write( mm.storage, wargs )
      }
    } yield {
      res
    }

    for (_ <- storWriteFut.failed)
      LOGGER.error("Failed to send to storage local image: " + loc)

    // Дождаться завершения всех паралельных операций.
    for {
      _   <- mnodeSaveFut
      _   <- storWriteFut
      mm  <- mediaSavedFut
    } yield {
      mm
    }
  }

  /** Существует ли картинка в хранилище? */
  override def existsInPermanent(mimg: MImgT): Future[Boolean] = {
    val isExistsFut = _mediaFut( _mediaOptFut(mimg) )
      .flatMap { mmedia =>
        iMediaStorages.isExist( mmedia.storage )
      }

    // возвращать false при ошибках.
    val resFut = isExistsFut.recover {
      // Если подавлять все ошибки связи, то система будет удалять все local imgs.
      case _: NoSuchElementException =>
        false
    }
    // Залоггировать неожиданные экзепшены.
    for (ex <- isExistsFut.failed) {
      if (!ex.isInstanceOf[NoSuchElementException])
        LOGGER.warn("existsInPermanent($mimg) or _mediaFut failed", ex)
    }
    resFut
  }

}


/** Статические расширения сборки инстансов модели [[MImg3]]. */
object MImg3 extends MacroLogsImpl with IMImgCompanion {

  override type T = MImg3

  /** Реализация парсеров filename'ов в данную модель. */
  class Parsers extends ImgFileNameParsersImpl {

    override type T = MImg3

    override def fileName2miP: Parser[T] = {
      (uuidStrP ~ imOpsP) ^^ {
        case nodeId ~ dynImgOps =>
          MImg3(nodeId, dynImgOps)
      }
    }

  }

  override def apply(fileName: String): MImg3 = {
    val pr = (new Parsers).fromFileName(fileName)
    if (!pr.successful)
      LOGGER.error(s"""Failed to parse img from fileName <<$fileName>>:\n$pr""")
    pr.get
  }

  /** Экстракция указателя на картинку из эджа узла N2.
    * @throws java.util.NoSuchElementException когда id узла-картинки не задан.
    */
  def apply(medge: MEdge): MImg3 = {
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
    MImg3(medge.nodeIds.head, dops)
  }

  def apply(mmedia: MMedia): MImg3 = {
    // TODO Безопасно ли? По идее да, но лучше потестить или использовать какие-то данные из иных мест.
    apply( mmedia.id.get )
  }

  override def fromImg(img: MAnyImgT, dynOps2: Option[List[ImOp]] = None): MImg3 = {
    MImg3(img.rowKeyStr, dynOps2.getOrElse(img.dynImgOps))
  }

}


/**
  * Класс элементов модели.
  * @param rowKeyStr Строковой ключ картинки-узла.
  * @param dynImgOps Список параметров трансформации картинки.
  * @param userFileName Имя файла, присланное юзером.
  */
case class MImg3(
  override val rowKeyStr            : String,
  override val dynImgOps            : Seq[ImOp],
  override val userFileName         : Option[String] = None
)
  extends MImgT
{

  override lazy val rowKey: UUID = {
    UuidUtil.base64ToUuid(rowKeyStr)
  }

  lazy val _mediaId = MMedia.mkId(rowKeyStr, qOpt)

  override def storage = MStorages.SeaWeedFs
  override type MImg_t = MImg3
  override def thisT: MImg_t = this
  override def toWrappedImg = this

  override def withDynOps(dynImgOps2: Seq[ImOp]): MImg3 = {
    copy(dynImgOps = dynImgOps2)
  }

}
