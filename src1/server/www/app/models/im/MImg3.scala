package models.im

import java.io.FileNotFoundException
import java.time.OffsetDateTime
import java.util.NoSuchElementException
import javax.inject.Inject
import io.suggest.common.geom.d2.ISize2di
import io.suggest.es.model.EsModel
import io.suggest.fio.{IDataSource, MDsReadArgs, WriteRequest}
import io.suggest.img
import io.suggest.img.{ImgSzDated, MImgFormat}
import io.suggest.n2.edge.{MEdge, MEdgeInfo, MNodeEdges, MPredicates}
import io.suggest.n2.media.storage.{IMediaStorages, MStorages}
import io.suggest.n2.media._
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.n2.node.common.MNodeCommon
import io.suggest.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.streams.StreamsUtil
import io.suggest.text.StringUtil
import io.suggest.up.UploadConstants
import io.suggest.util.logs.{MacroLogsImpl, MacroLogsImplLazy}
import util.img.ImgFileNameParsersImpl
import util.up.FileUtil
import japgolly.univeq._
import monocle.macros.GenLens
import play.api.inject.Injector

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 17:27
 * Description: Реализация модели [[MImgT]], вместо прямого взаимодействия с хранилищем.
 * [[MImgs3]] -- DI-реализация объекта-компаньона.
  *
 * TODO Надо удалить эту модель, дораспилив на MDynImgId + DynImgUtil.
 */
class MImgs3 @Inject() (
                         override val injector: Injector,
                       )
  extends MAnyImgsT[MImgT]
  with MacroLogsImplLazy
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val iMediaStorages = injector.instanceOf[IMediaStorages]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val fileUtil = injector.instanceOf[FileUtil]
  private lazy val streamsUtil = injector.instanceOf[StreamsUtil]
  private lazy val mLocalImgs = injector.instanceOf[MLocalImgs]

  import esModel.api._

  override def delete(mimg: MImgT): Future[_] = {
    mediaOptFut(mimg).flatMap {
      case Some(mm) =>
        (for {
          edge      <- _fileEdge( mm )
          edgeMedia <- edge.media
          storage   <- edgeMedia.storage
        } yield {
          for {
            _ <- iMediaStorages
              .client( storage.storage )
              .delete( storage.data )
            _ <- mNodes.deleteById( mm.id.get )
          } yield {
            true
          }
        })
          .getOrElse {
            Future.successful(false)
          }

      case None =>
        Future.successful(false)
    }
  }


  override def getDataSource(mimg: MImgT): Future[IDataSource] = {
    for {
      mm <- _mediaFut( mediaOptFut(mimg) )
      stor = _fileEdge(mm)
        .flatMap(_.media)
        .flatMap(_.storage)
        .get
      rr <- iMediaStorages
        .client( stor.storage )
        .read( MDsReadArgs(stor.data) )
    } yield {
      rr
    }
  }


  /** Убедится, что в хранилищах существует сохраненный экземпляр MNode.
    * Если нет, то создрать и сохранить. */
  def ensureMnode(mimg: MImgT): Future[MNode] = {
    mNodes
      .getByIdCache( mimg.dynImgId.origNodeId )
      .map(_.get)
      .recoverWith { case _: NoSuchElementException =>
        saveMnode(mimg)
      }
  }

  /** Сгенерить новый экземпляр MNode и сохранить. */
  def saveMnode(mimg: MImgT): Future[MNode] = {
    val permFut = imgMetaData(mimg)
    val fname = mimg.userFileName.getOrElse {
      mLocalImgs.generateFileName( mimg.toLocalInstance )
    }

    val mnodeFut = for {
      perm    <- permFut
    } yield {
      // Собираем новый узел n2, когда все необходимые данные уже собраны...
      MNode(
        id = Some( mimg.dynImgId.origNodeId ),
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
    mnodeFut
      .flatMap( mNodes.saveReturning(_) )
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
        LOGGER.trace(s"$logPrefix Node already exist: $res")
    }

    val imgFile = mLocalImgs.fileOf(loc)

    // TODO Ассигновать картинку на том же узле sio, что и оригинал. Надо удалить весь этот метод, чтобы руление картинками шло вне модели, в DynImgs, например.

    lazy val storClient = iMediaStorages.client( mimg.storage )

    // Вернуть экземпляр MNode.
    val mediaSavedFut: Future[MNode] = media0Fut.recoverWith { case _: NoSuchElementException =>
      // Перезаписывать нечего, т.к. узел ещё не существует.
      val whOptFut = mLocalImgs.getImageWH(loc)
      // TODO Допустить, что хэши уже просчитаны где-то в контроллере, не считать их тут...
      val hashesHexFut = fileUtil.mkHashesHexAsync(
        file   = imgFile,
        hashes = UploadConstants.CleverUp.UPLOAD_FILE_HASHES,
        flags  = MFileMetaHashFlags.ORIGINAL_FLAGS,
      )

      val storAssignFut = storClient.assignNew()

      val szB = imgFile.length()

      for {
        whOpt       <- whOptFut
        mime        <- mimeFut
        hashesHex   <- hashesHexFut
        stor        <- storAssignFut
        mnode = MNode(
          id = Some( mimg.mediaId ),
          edges = MNodeEdges(
            out = {
              val fileEdge = MEdge(
                predicate = MPredicates.Blob.File,
                nodeIds = mimg.dynImgId
                  .maybeOriginal
                  .map(_.mediaId)
                  .toSet,
                media = Some(MEdgeMedia(
                  file = MFileMeta(
                    mime        = Some( mime ),
                    sizeB       = Some( szB ),
                    isOriginal  = !mimg.dynImgId.hasImgOps,
                    hashesHex   = hashesHex
                  ),
                  picture = MPictureMeta(
                    whPx = whOpt
                  ),
                  storage = Some(stor.storage),
                )),
                info = MEdgeInfo(
                  // Эта дата используется для Last-Modified.
                  dateNi = Some( OffsetDateTime.now() ),
                )
              )

              fileEdge :: Nil
            }
          ),
          common = MNodeCommon(
            ntype         = MNodeTypes.Media.Image,
            isDependent   = true,
          )
        )
        mediaMeta2    <- mNodes.save( mnode )
      } yield {
        val metaId = mediaMeta2.id.get
        assert( mnode.id contains[String] metaId )
        mNodes.putToCache( mnode )
        LOGGER.info(s"$logPrefix Saved to permanent: media#$metaId")
        mnode
      }
    }

    // Параллельно запустить поиск и сохранение экземпляра MNode.
    val mnodeSaveFut = ensureMnode(mimg)

    for (_ <- mnodeSaveFut.failed)
      LOGGER.error(s"$logPrefix Failed to save picture MNode for local img " + loc)

    // Выполнить заливку файла в постоянное надежное хранилище:
    val storWriteFut = for {
      mnode   <- mediaSavedFut
      res     <- (for {
        edge      <- mnode.edges
          .withPredicateIter( MPredicates.Blob.File )
          .nextOption()
        edgeMedia <- edge.media
        storage   <- edgeMedia.storage
      } yield {
        val wargs = WriteRequest(
          contentType = edgeMedia.file.mime.get,
          file        = imgFile,
        )
        storClient.write( storage.data, wargs )
      })
        .get
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
      .flatMap { mnode =>
        (for {
          fileEdge <- mnode.edges
            .withPredicateIter( MPredicates.Blob.File )
            .nextOption()
          edgeMedia <- fileEdge.media
          s <- edgeMedia.storage
        } yield {
          iMediaStorages
            .client( s.storage )
            .isExist( s.data )
        })
          .getOrElse( Future.successful(false) )
      }

    // Залоггировать неожиданные экзепшены.
    for (ex <- isExistsFut.failed) {
      if (!ex.isInstanceOf[NoSuchElementException])
        LOGGER.warn("existsInPermanent($mimg) or _mediaFut failed", ex)
    }

    // возвращать false при ошибках.
    isExistsFut.recover {
      // Если подавлять все ошибки связи, то система будет удалять все local imgs.
      case _: NoSuchElementException =>
        false
    }
  }


  // Сюда замёржен MImgT:

  private def mediaOptFut(mimg: MImgT): Future[Option[MNode]] =
    mNodes.getByIdCache( mimg.dynImgId.mediaId )

  private def _fileEdge(mnode: MNode) = {
    mnode.edges
      .withPredicateIter( MPredicates.Blob.File )
      .nextOption()
  }

  private def _mediaFut(mediaOptFut: Future[Option[MNode]]): Future[MNode] = {
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

  /** Сохранённые в узле  метаданные картинки. */
  def imgMetaData(mimg: MImgT): Future[Option[ImgSzDated]] = {
    for (mmediaOpt <- mediaOptFut(mimg)) yield {
      for {
        mnode   <- mmediaOpt
        fEdge   <- _fileEdge( mnode )
        eMedia  <- fEdge.media
        whPx    <- eMedia.picture.whPx
      } yield {
        img.ImgSzDated(
          sz          = whPx,
          dateCreated = mnode.meta.basic.dateCreated,
        )
      }
    }
  }

  /** Получить ширину и длину картинки. */
  override def getImageWH(mimg: MImgT): Future[Option[ISize2di]] = {
    // Ищем уже определённые ранее данные по картинке.
    (for {
      metaOpt <- imgMetaData(mimg)
    } yield {
      metaOpt.map(_.sz)
    })
  }

  override def rawImgMeta(mimg: MImgT): Future[Option[ImgSzDated]] = {
    imgMetaData(mimg)
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
      (uuidStrP ~ dotImgFormatOptP ~ imOpsP) ^^ {
        case nodeId ~ imgFormatOpt ~ imgOps =>
          MImg3(MDynImgId(nodeId, imgFormatOpt, imgOps))
      }
    }

  }

  def parse(fileName: String) =
    (new Parsers).fromFileName(fileName)

  override def apply(fileName: String): MImg3 = {
    val pr = parse( fileName )
    if (!pr.successful)
      LOGGER.error(s"""Failed to parse img from fileName <<$fileName>>:\n$pr""")
    pr.get
  }


  /** Сброка инстанса MImg3 на основе эджа. */
  def fromEdge(nodeId: String, e: MEdge): Option[MImg3] = {
    for {
      edgeMedia <- e.media
      imgFormat2 <- edgeMedia.file.imgFormatOpt
      parseResult = parse( nodeId )
      mimg0 <- parseResult
        .map( Some.apply )
        .getOrElse {
          LOGGER.warn(s"fromEdge(): Cannot parse imgId: $nodeId\n edge = $e\n$parseResult")
          None
        }
    } yield {
      // Костыль для выставления формата в картинку. Надо разобраться в актуальности этого действия:
      if (!(mimg0.dynImgId.imgFormat contains[MImgFormat] imgFormat2)) {
        MImg3.dynImgId
          .andThen( MDynImgId.imgFormat )
          .replace( edgeMedia.file.imgFormatOpt )(mimg0)
      } else {
        mimg0
      }
    }
  }


  override def fromImg(img: MAnyImgT, dynOps2: Option[List[ImOp]] = None): MImg3 = {
    val dynImgId0 = img.dynImgId
    val dynImgId2 = dynOps2.fold(dynImgId0) { MDynImgId.dynImgOps.replace(_)(dynImgId0) }

    MImg3( dynImgId2 )
  }


  def dynImgId      = GenLens[MImg3](_.dynImgId)
  def userFileName  = GenLens[MImg3](_.userFileName)

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
      .andThen( MDynImgId.dynImgOps )
      .replace( dynImgOps2 )(this)
  }

  def withDynImgId(dynImgId: MDynImgId) = copy(dynImgId = dynImgId)

  override def toString: String = StringUtil.toStringHelper(this) { helperF =>
    val helperF2 = helperF("")
    helperF2( dynImgId )
    userFileName foreach helperF2
  }

}
