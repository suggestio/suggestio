package models.im

import java.io.FileNotFoundException
import java.time.OffsetDateTime
import java.util.NoSuchElementException

import javax.inject.{Inject, Singleton}
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.es.model.EsModel
import io.suggest.fio.{IDataSource, WriteRequest}
import io.suggest.img
import io.suggest.img.ImgSzDated
import io.suggest.js.UploadConstants
import io.suggest.model.n2.media.storage.{IMediaStorages, MStorages}
import io.suggest.model.n2.media._
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.playx.CacheApiUtil
import io.suggest.streams.StreamsUtil
import io.suggest.util.logs.{MacroLogsImpl, MacroLogsImplLazy}
import models.mproj.ICommonDi
import util.img.ImgFileNameParsersImpl
import util.up.FileUtil
import japgolly.univeq._
import monocle.macros.GenLens

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 17:27
 * Description: Реализация модели [[MImgT]] на базе MMedia, вместо прямого взаимодействия с кассандрой.
 * [[MImgs3]] -- DI-реализация объекта-компаньона.
 */
@Singleton
class MImgs3 @Inject() (
                         esModel                   : EsModel,
                         iMediaStorages            : IMediaStorages,
                         mMedias                   : MMedias,
                         mNodes                    : MNodes,
                         fileUtil                  : FileUtil,
                         streamsUtil               : StreamsUtil,
                         cacheApiUtil              : CacheApiUtil,
                         mLocalImgs                : MLocalImgs,
                         override val mCommonDi    : ICommonDi,
                       )
  extends MAnyImgsT[MImgT]
  with MacroLogsImplLazy
{

  import mCommonDi._
  import esModel.api._

  override def delete(mimg: MImgT): Future[_] = {
    mediaOptFut(mimg).flatMap {
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


  override def getDataSource(mimg: MImgT): Future[IDataSource] = {
    for {
      mm <- _mediaFut( mediaOptFut(mimg) )
      rr <- iMediaStorages.read( mm.storage )
    } yield {
      rr
    }
  }

  private def _getImgMeta(mimg: MImgT): Future[Option[ImgSzDated]] = {
    for (mmediaOpt <- mediaOptFut(mimg)) yield {
      for (mmedia <- mmediaOpt; whPx <- mmedia.picture.whPx) yield {
        img.ImgSzDated(
          sz          = whPx,
          dateCreated = mmedia.file.dateCreated
        )
      }
    }
  }

  /** Потенциально ненужная операция обновления метаданных. В новой архитектуре её быть не должно бы,
    * т.е. метаданные обязательные изначально. */
  private def _updateMetaWith(mimg: MImgT, localWh: ISize2di, localImg: MLocalImg): Unit = {
    // should never happen
    // Необходимость апдейта метаданных возникает, когда обнаруживается, что нет метаданных.
    // В случае N2 MMedia, метаданные без блоба существовать не могут, и необходимость не должна наступать.
    LOGGER.warn(s"_updateMetaWith($localWh, $localImg) ignored and not implemented")
  }


  /** Убедится, что в хранилищах существует сохраненный экземпляр MNode.
    * Если нет, то создрать и сохранить. */
  def ensureMnode(mimg: MImgT): Future[MNode] = {
    mNodes
      .getByIdCache( mimg.dynImgId.rowKeyStr )
      .map(_.get)
      .recoverWith { case _: NoSuchElementException =>
        saveMnode(mimg)
      }
  }

  /** Сгенерить новый экземпляр MNode и сохранить. */
  def saveMnode(mimg: MImgT): Future[MNode] = {
    val permFut = permMetaCached(mimg)
    val fname = mimg.userFileName.getOrElse {
      mLocalImgs.generateFileName( mimg.toLocalInstance )
    }
    val mnodeFut = for {
      perm    <- permFut
    } yield {
      // Собираем новый узел n2, когда все необходимые данные уже собраны...
      MNode(
        id = Some( mimg.dynImgId.rowKeyStr ),
        common = MNodeCommon(
          ntype         = MNodeTypes.Media.Image,
          isDependent   = true,
        ),
        meta = MMeta(
          basic = MBasicMeta(
            techName    = Some( fname ),
            dateCreated = perm
              .fold( OffsetDateTime.now() )(_.dateCreated),
          ),
        ),
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

  protected def _doSaveToPermanent(mimg: MImgT): Future[_] = {
    val loc = mimg.toLocalInstance
    val mimeFut = mLocalImgs.mimeFut(loc)
    val media0Fut = _mediaFut {
      mediaOptFut( mimg )
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
      val hashesHexFut = fileUtil.mkHashesHexAsync(
        file   = imgFile,
        hashes = UploadConstants.CleverUp.PICTURE_FILE_HASHES,
        flags  = Set(MFileMetaHash.Flags.TRULY_ORIGINAL),
      )
      // TODO Ассигновать картинку на том же узле sio, что и оригинал. Надо удалить весь этот метод, чтобы руление картинками шло вне модели, в DynImgs, например.
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
          nodeId  = mimg.dynImgId.rowKeyStr,
          id      = Some( mimg.mediaId ),
          file    = MFileMeta(
            mime        = mime,
            sizeB       = szB,
            isOriginal  = !mimg.dynImgId.hasImgOps,
            hashesHex   = hashesHex
          ),
          picture = MPictureMeta(
            whPx = whOpt
          ),
          storage = stor.storage,
        )
      }
    }

    val mediaSavedFut = media0Fut.recoverWith { case _: Throwable =>
      for {
        mmedia      <- mediaFut
        mediaId2    <- mMedias.save(mmedia)
      } yield {
        assert( mmedia.id contains[String] mediaId2 )
        mMedias.putToCache( mmedia )
        LOGGER.info(s"$logPrefix Saved to permanent: media#$mediaId2")
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
      LOGGER.error(s"$logPrefix Failed to send to storage local image: $loc")

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
  def existsInPermanent(mimg: MImgT): Future[Boolean] = {
    val isExistsFut = _mediaFut( mediaOptFut(mimg) )
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


  // Сюда замёржен MImgT:

  def mediaOptFut(mimg: MImgT): Future[Option[MMedia]] = {
    mMedias.getByIdCache(mimg.dynImgId.mediaId)
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
      // Готовим поточное чтение из стораджа:
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
            if (mimg.dynImgId.hasImgOps) {
              LOGGER.debug(s"$logPrefix non-orig img not in permanent storage: $toFile")
            } else {
              def msg = s"$logPrefix img not found in permanent storage: $toFile"
              if (ex.isInstanceOf[NoSuchElementException]) LOGGER.debug(msg)
              else LOGGER.debug(msg, ex)
            }
          }
        } else {
          LOGGER.warn(s"$logPrefix _getImgBytes2 or writeIntoFile $toFile failed", ex)
        }
        None
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

  /** Получить ширину и длину картинки. */
  override def getImageWH(mimg: MImgT): Future[Option[ISize2di]] = {
    // Фетчим паралельно из обеих моделей. Кто первая, от той и принимаем данные.
    val mimg2Fut = for {
      metaOpt <- permMetaCached(mimg)
      meta = metaOpt.get
    } yield {
      Some( meta.sz )
    }

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

}


/** Статические расширения сборки инстансов модели [[MImg3]]. */
object MImg3 extends MacroLogsImpl with IMImgCompanion {

  override type T = MImg3

  /** Реализация парсеров filename'ов в данную модель. */
  class Parsers extends ImgFileNameParsersImpl {

    override type T = MImg3

    override def fileName2miP: Parser[T] = {
      // TODO Использовать парсер, делающий сразу MDynImgId
      (uuidStrP ~ dotDynFormatOrJpegP ~ imOpsP) ^^ {
        case nodeId ~ dynFormat ~ dynImgOps =>
          MImg3(MDynImgId(nodeId, dynFormat, dynImgOps))
      }
    }

  }

  override def apply(fileName: String): MImg3 = {
    val pr = (new Parsers).fromFileName(fileName)
    if (!pr.successful)
      LOGGER.error(s"""Failed to parse img from fileName <<$fileName>>:\n$pr""")
    pr.get
  }

  /** Извлечение данных картинки из MMedia. */
  def apply(mmedia: MMedia): MImg3 = {
    val dynFmt = mmedia.file.imgFormatOpt.get
    // TODO Безопасно ли? По идее да, но лучше потестить или использовать какие-то данные из иных мест.
    val mimg0 = apply( mmedia.id.get )
    if (mimg0.dynImgId.dynFormat !=* dynFmt) {
      MImg3.dynImgId
        .composeLens( MDynImgId.dynFormat )
        .set( dynFmt )(mimg0)
    } else {
      mimg0
    }
  }

  override def fromImg(img: MAnyImgT, dynOps2: Option[List[ImOp]] = None): MImg3 = {
    val dynImgId0 = img.dynImgId
    val dynImgId2 = dynOps2.fold(dynImgId0) { MDynImgId.dynImgOps.set(_)(dynImgId0) }

    MImg3( dynImgId2 )
  }


  val dynImgId      = GenLens[MImg3](_.dynImgId)
  val userFileName  = GenLens[MImg3](_.userFileName)

}


/**
  * Класс элементов модели.
  * @param userFileName Имя файла, присланное юзером.
  */
case class MImg3(
                  override val dynImgId             : MDynImgId,
                  // TODO userFileName удалить следом за saveToPermanent().
                  //      Это костыль у старой заливки картинок. В DistImg весь аплоад уже в норме.
                  override val userFileName         : Option[String]  = None
                )
  extends MImgT
{

  // TODO Удалить это
  def mediaId = dynImgId.mediaId

  override def storage = MStorages.SeaWeedFs
  override type MImg_t = MImg3
  override def thisT: MImg_t = this
  override def toWrappedImg = this

  override def withDynOps(dynImgOps2: Seq[ImOp]): MImg3 = {
    MImg3.dynImgId
      .composeLens( MDynImgId.dynImgOps )
      .set( dynImgOps2 )(this)
  }

  def withDynImgId(dynImgId: MDynImgId) = copy(dynImgId = dynImgId)

}
