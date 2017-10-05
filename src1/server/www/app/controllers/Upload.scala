package controllers

import java.io.File
import java.nio.file.Path
import javax.inject.{Inject, Singleton}

import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.IMust
import io.suggest.file.MSrvFileInfo
import io.suggest.file.up.{MFile4UpProps, MUploadResp}
import io.suggest.fio.WriteRequest
import io.suggest.i18n.MMessage
import io.suggest.js.UploadConstants
import io.suggest.model.n2.media._
import io.suggest.model.n2.media.search.{MHashCriteria, MMediaSearchDfltImpl}
import io.suggest.model.n2.media.storage.swfs.{SwfsStorage, SwfsStorages}
import io.suggest.model.n2.media.storage.{IMediaStorages, MStorages}
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.pick.MimeConst
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.svg.SvgUtil
import io.suggest.swfs.client.proto.fid.Fid
import models.im.{MImg3, MLocalImg, MLocalImgs}
import models.mproj.ICommonDi
import models.mup.{MUploadFileHandler, MUploadFileHandlers, MUploadTargetQs}
import models.req.IReq
import net.sf.jmimemagic.Magic
import play.api.libs.Files.{SingletonTemporaryFileCreator, TemporaryFile, TemporaryFileCreator}
import play.api.libs.json.Json
import play.api.mvc.{BodyParser, MultipartFormData, Result}
import play.core.parsers.Multipart
import util.acl.CanUploadFile
import util.cdn.CdnUtil
import util.up.{FileUtil, UploadUtil}
import views.html.helper.CSRF
import japgolly.univeq._
import util.img.ImgFileUtil

import scala.concurrent.Future
import scala.util.Try
import scalaz.ValidationNel

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.10.17 12:26
  * Description: Контроллер загрузки файлов на сервера s.io.
  */
@Singleton
class Upload @Inject()(
                        mMedias                   : MMedias,
                        uploadUtil                : UploadUtil,
                        canUploadFile             : CanUploadFile,
                        cdnUtil                   : CdnUtil,
                        fileUtil                  : FileUtil,
                        val iMediaStorages        : IMediaStorages,
                        mNodes                    : MNodes,
                        mLocalImgs                : MLocalImgs,
                        imgFileUtil               : ImgFileUtil,
                        override val mCommonDi    : ICommonDi
                      )
  extends SioControllerImpl
  with MacroLogsImpl
{

  import mCommonDi.{csrf, ec}


  /** Body-parser для prepareUploadLogic. */
  def prepareUploadBp = parse.json[MFile4UpProps]

  /** Тело экшена подготовки к аплоаду.
    * Только тело, потому что ACL-проверки выносятся в основной контроллер, в контексте которого происходит загрузка.
    *
    * @param validated Провалидированные JSON-метаданные файла.
    * @param uploadFileHandler Если требуется принимать файл не в /tmp/, а сразу куда-то, то здесь Some().
    *
    * @return Created | Accepted | NotAcceptable  + JSON-body в формате MFile4UpProps.
    */
  def prepareUploadLogic(logPrefix: String, validated: ValidationNel[String, MFile4UpProps],
                         uploadFileHandler: Option[MUploadFileHandler] = None)
                        (implicit request: IReq[MFile4UpProps]) : Future[Result] = {
    validated.fold(
      // Ошибка валидации присланных данных. Вернуть ошибку клиенту.
      {errorsNel =>
        LOGGER.warn(s"$logPrefix Failed to verify body: ${errorsNel.iterator.mkString(", ")}\n ${request.body}")
        val resp = MUploadResp(
          errors = errorsNel.iterator.map { msg => MMessage(msg) }.toSeq
        )
        NotAcceptable( Json.toJson(resp) )
      },

      // Успешно провалидированы данные файла для загрузки.
      {upFileProps =>
        LOGGER.trace(s"$logPrefix Body validated, user#${request.user.personIdOpt.orNull}:\n ${request.body} => $upFileProps")

        // Нужно поискать файл с такими параметрами в MMedia:
        val sameFileSearch = new MMediaSearchDfltImpl {
          override def fileSizeB = upFileProps.sizeB :: Nil
          override def fileHashesHex = {
            upFileProps.hashesHex
              .iterator
              .map { case (mhash, hexValue) =>
                MHashCriteria(
                  hTypes    = mhash :: Nil,
                  hexValues = hexValue :: Nil,
                  must      = IMust.MUST
                )
              }
              .toSeq
          }
          override def limit = 1
        }
        val fileSearchResFut = mMedias.dynSearch( sameFileSearch )

        // Собрать ответ с результатом поиска
        fileSearchResFut.flatMap { fileSearchRes =>
          val (respStatus, respDataFut) = fileSearchRes
            .headOption
            .fold [(Status, Future[MUploadResp])] {
              val storageType = MStorages.SeaWeedFs
              val storageFacade = iMediaStorages.getModel( storageType ).asInstanceOf[SwfsStorages]
              val assignRespFut = storageFacade.assignNew()
              LOGGER.trace(s"$logPrefix No existing file, user will upload a new file.")

              val upDataFut = for {
                assignResp <- assignRespFut
              } yield {
                val swfsAssignResp = assignResp._2
                LOGGER.trace(s"$logPrefix Assigned swfs resp: $assignResp")
                val upData = MUploadTargetQs(
                  hashesHex   = upFileProps.hashesHex,
                  mimeType    = upFileProps.mimeType,
                  fileSizeB   = upFileProps.sizeB,
                  fileHandler = uploadFileHandler,
                  personId    = request.user.personIdOpt,
                  validTillS  = uploadUtil.ttlFromNow(),
                  storage     = storageType,
                  storHost    = swfsAssignResp.url,
                  storInfo    = swfsAssignResp.fid
                )
                // Список хостнеймов: в будущем возможно, что ссылок для заливки будет несколько: основная и запасная. Или ещё что-то.
                val hostnames = Seq(
                  swfsAssignResp.publicUrl
                  // TODO Вписать запасные хостнеймы для аплоада?
                )
                MUploadResp(
                  // IMG_DIST: URL включает в себя адрес ноды, на которую заливать.
                  upUrls = for (host <- hostnames) yield {
                    "//" + host + CSRF(routes.Upload.doFileUpload(upData)).url
                  }
                )
              }
              (Created, upDataFut)

            } { foundFile =>
              LOGGER.debug(s"$logPrefix Found existing file: $foundFile for props $upFileProps")
              val upResp = MUploadResp(
                fileExist = Some(MSrvFileInfo(
                  nodeId    = foundFile.nodeId,
                  // TODO Сгенерить ссылку на файл. Если это картинка, то через dynImgArgs
                  url       = if ( MimeConst.Image.isImage(foundFile.file.mime) ) {
                    // TODO IMG_DIST: Вписать хост расположения картинки.
                    // TODO Нужна ссылка картинки на недо-оригинал картинки? Или как?
                    routes.Img.dynImg( MImg3(foundFile) ).url
                  } else {
                    // TODO IMG_DIST Надо просто универсальную ссылку для скачивания файла, независимо от его типа.
                    throw new UnsupportedOperationException(s"MIME ${foundFile.file.mime} don't know how to build URL")
                  },
                  sizeB     = Some( foundFile.file.sizeB ),
                  name      = None,     // Имя пока не раскрываем. Файл мог быть был загружен другим юзером под иным именем.
                  mimeType  = Some( foundFile.file.mime ),
                  hashesHex = MFileMetaHash.toHashesHex( foundFile.file.hashesHex )
                ))
              )
              (Accepted, Future.successful(upResp))
            }

          for (respData <- respDataFut) yield {
            respStatus( Json.toJson(respData) )
          }
        }
      }
    )
  }


  /** BodyParser для запросов, содержащих файлы для аплоада. */
  private def _uploadFileBp(uploadArgs: MUploadTargetQs): BodyParser[UploadBpRes] = {
    // Если картинка, то заливать сразу в MLocalImg/ORIG.
    val fileHandler = uploadArgs.fileHandler
      .fold[TemporaryFileCreator] (SingletonTemporaryFileCreator) {
        // Команда к сохранению напрямую в MLocalImg: сообрать соответствующий FileHandler.
        case MUploadFileHandlers.Picture =>
          new LocalImgFileCreator( MLocalImg() )
      }
    // Сборка самого BodyParser'а.
    val bp0 = parse.multipartFormData(
      Multipart.handleFilePartAsTemporaryFile( fileHandler ),
      maxLength = uploadArgs.fileSizeB + 10000L
    )
    // Завернуть итог парсинга в контейнер, содержащий данные MLocalImg или иные возможные поля.
    for (mpfd <- bp0) yield {
      new UploadBpRes(
        data = mpfd,
        localImg = fileHandler match {
          case lifc: LocalImgFileCreator => Some( lifc.mLocalImg )
          case _ => None
        }
      )
    }
  }


  /** Экшен фактического аплоада файла с клиента.
    * Файл приходит как часть multipart.
    */
  def doFileUpload(uploadArgs: MUploadTargetQs) = csrf.Check {
    val bp = _uploadFileBp(uploadArgs)

    canUploadFile(uploadArgs).async(bp) { implicit request =>

      lazy val logPrefix = s"doFileUpload()#${System.currentTimeMillis()}:"
      LOGGER.trace(s"$logPrefix Starting w/args: $uploadArgs")

      lazy val errSb = new StringBuilder()
      def __appendErr(msg: String): Unit = {
        LOGGER.warn(s"$logPrefix $msg")
        errSb.append(msg).append('\n')
      }

      // Начинаем с синхронных проверок:
      val resFutOpt: Option[Future[Result]] = for {
        // Поискать файл в теле запроса:
        filePart <- request.body.data.file( UploadConstants.MPART_FILE_FN )

        // Проверить Content-Type, заявленный в теле запроса:
        if {
          val r = filePart.contentType
            .exists(_ equalsIgnoreCase uploadArgs.mimeType)
          if (!r)
            __appendErr( s"Expected Content-type '${uploadArgs.mimeType}' not matching to Multipart part's content-type '${filePart.contentType.orNull}'." )
          r
        }

        srcPath = filePart.ref.path
        srcFile = srcPath.toFile

        // Сверить размер файла с заявленным размером
        if {
          val srcLen = srcFile.length()
          val r = srcLen ==* uploadArgs.fileSizeB
          if (!r)
            __appendErr(s"Size of file $srcLen bytes, but expected is ${uploadArgs.fileSizeB} bytes.")
          r
        }

        // Вычислить фактический mime-тип файла.
        mimeType <- try {
          val srcMagicMatch = Magic.getMagicMatch(srcFile, false)
          Option( srcMagicMatch.getMimeType )
        } catch {
          case ex: Throwable =>
            val msg = s"Failed to detect MIME type of file."
            LOGGER.error(s"$logPrefix $msg '$srcFile'", ex)
            __appendErr(msg)
            None
        }

        // Сравнить фактический MIME-тип с заявленным.
        if {
          val r = mimeType ==* uploadArgs.mimeType
          if (!r)
            __appendErr( s"Detected file MIME type '$mimeType' does not match to expected ${uploadArgs.mimeType}." )
          r
        }

      } yield {
        // Синхронные проверки завершены успешно. Переходим в асинхрон:
        for {

          // Сверить чек-суммы файла, все и параллельно.
          hashesHex2 <- for {
            hashesHexIterable2 <- Future.traverse( uploadArgs.hashesHex ) {
              case (mhash, expectedHexVal) =>
                Future( fileUtil.mkFileHash(mhash, srcFile) )
                  .filter { srcHash =>
                    val r = srcHash equalsIgnoreCase expectedHexVal
                    if (!r) errSb.synchronized {
                      __appendErr( s"File hash ${mhash.fullStdName} '$srcHash' doesn't match to declared '$expectedHexVal'." )
                    }
                    r
                  }
                  .map { MFileMetaHash(mhash, _) }
            }
          } yield {
            hashesHexIterable2.toSeq
          }

          // TODO SEC Проверить полученный файл антивирусом: clamd + clamdscan --fdpass $file

          // Проверить валидность принятой картинки с помощью identify:
          imgIdentifyInfoOpt <- FutureUtil.optFut2futOpt(request.body.localImg) { mLocImg =>
            for {
              info <- mLocalImgs.identifyCached( mLocImg )
              // По идее, возможная ошибка уже должна быть выявлена.
              // На всякий случай дополнительно проверяем info != null:
              infoOpt = Option(info)
              if infoOpt.nonEmpty
            } yield {
              infoOpt
            }
          }

          // Ожидалась картинка?
          isImg = imgIdentifyInfoOpt.nonEmpty

          // Это svg-картинка?
          isImgIsSvg = imgIdentifyInfoOpt.exists(_.getImageFormat equalsIgnoreCase "svg")

          // Если тут SVG, то убедиться в валидности принятой картинки.
          if {
            val isValidIfSvg = !isImgIsSvg || SvgUtil.isSvgFileValid( srcFile )
            if (!isValidIfSvg) errSb.synchronized {
              __appendErr( s"SVG-file looks invalid." )
            }
            isValidIfSvg
          }

          fileNameOpt = Option( filePart.filename )

          // Собираем MediaStorage ptr:
          mediaStor = uploadArgs.storage match {
            case MStorages.SeaWeedFs => SwfsStorage( Fid(uploadArgs.storInfo) )
          }

          // Запускаем в фоне заливку файла из ФС в надёжное распределённое хранилище:
          saveFileToShardFut = {
            val wr = WriteRequest(
              contentType  = mimeType,
              file         = srcFile,
              origFileName = fileNameOpt
            )
            iMediaStorages
              .write( mediaStor, wr )
          }

          // Создаём новый узел для загруженного файла.
          mnodeId <- {
            // Проверки закончены. Пора переходить к действиям по сохранению и анализу файла.
            val nodeIdOpt0 = request.body.localImg.map(_.rowKeyStr)
            val MediaTypes = MNodeTypes.Media
            val mnode0 = MNode(
              id = nodeIdOpt0,
              common = MNodeCommon(
                ntype         = if (isImg) {
                  MediaTypes.Image
                } else {
                  LOGGER.info(s"$logPrefix Node will be created as OtherFile: no ideas here, mime=$mimeType")
                  MediaTypes.OtherFile
                },
                isDependent   = true
              ),
              meta = MMeta(
                basic = MBasicMeta(
                  techName    = Option( filePart.filename )
                )
              )
            )
            val fut = mNodes.save( mnode0 )

            // При ошибке сохранения узла надо удалить файл из saveFileToShardFut
            for {
              _ <- fut.failed
              _ <- {
                LOGGER.error(s"$logPrefix Failed to save MNode#${nodeIdOpt0.orNull}. Deleting file storage#$mediaStor...")
                saveFileToShardFut
              }
              delFileRes <- iMediaStorages.delete(mediaStor)
            } {
              LOGGER.warn(s"$logPrefix Emergency deleted ok file#$mediaStor => $delFileRes")
            }

            fut
          }

          // Наконец, сохраняем в MMedia:
          mmediaId <- {
            val mimg3Opt = for (_ <- imgIdentifyInfoOpt) yield
              MImg3( mnodeId, Nil, fileNameOpt )
            val mediaIdOpt0 = mimg3Opt.map(_._mediaId)
            val mmedia0 = MMedia(
              nodeId = mnodeId,
              id   = mediaIdOpt0,
              file = MFileMeta(
                mime       = mimeType,
                sizeB      = uploadArgs.fileSizeB,
                isOriginal = true,
                hashesHex  = hashesHex2
              ),
              picture = imgIdentifyInfoOpt
                .map(imgFileUtil.identityInfo2wh)
                .map(MPictureMeta.apply),
              storage = mediaStor
            )
            val fut = mMedias.save( mmedia0 )

            // При ошибке сохранения MMedia надо удалить узел и файл:
            for {
              ex <- fut.failed
              delNodeResFut = {
                LOGGER.error(s"$logPrefix Failed to save MMedia, deleting file storage#$mediaStor and MNode#$mnodeId ...", ex)
                mNodes.deleteById( mnodeId )
              }
              _ <- saveFileToShardFut
              delFileRes <- iMediaStorages.delete(mediaStor)
              delNodeRes <- delNodeResFut
            } {
              LOGGER.warn(s"$logPrefix Emergency deleted file storage#$mediaStor=>$delFileRes and MNode#$mnodeId=>$delNodeRes")
            }

            fut
          }

          // Дожидаемся окончания заливки файла в хранилище:
          _ <- {
            // При ошибке заливки файла, надо удалять созданный узел и запись в MMedia:
            for {
              ex <- saveFileToShardFut.failed
              delNodeResFut = {
                LOGGER.error(s"$logPrefix Failed to send file into storage#$mediaStor . Deleting MNode#$mnodeId and mmedia#$mmediaId ...", ex)
                mNodes.deleteById( mnodeId )
              }
              delMediaRes <- mMedias.deleteById( mmediaId )
              delNodeRes  <- delNodeResFut
            } {
              LOGGER.warn(s"$logPrefix Emergency deleted MNode#$mnodeId=>$delNodeRes and mmedia#$mmediaId=>$delMediaRes")
            }

            saveFileToShardFut
          }

        } yield {
          // Процедура проверки и сохранения аплоада завершёна успешно!
          // TODO Запустить анализ цветов, если требуется + тут картинка + задан ctxId.

          // Вернуть 200 Ok с данными по файлу
          val resp = MSrvFileInfo(
            nodeId    = mnodeId,
            url       = if (isImg) {
              "TODO.need.good.abs.img.link"
            } else {
              LOGGER.error(s"$logPrefix TODO URL-generation not implemented for !isImg")
              "TODO/NotImpl"
            },
            // Нет необходимости слать это всё назад, поэтому во всех заведомо известных клиенту поля None или empty:
            sizeB     = None,
            name      = None,
            mimeType  = None,
            hashesHex = Map.empty
          )
          Ok( Json.toJson(resp) )
        }
      }

      resFutOpt.fold [Future[Result]] {
        val errMsg = errSb.toString()
        LOGGER.warn(s"$logPrefix Failed to sync-validate upload data:\n$errMsg")
        NotAcceptable(s"Problems:\n\n$errMsg")
      } { resFut =>
        resFut.recover { case ex: Throwable =>
          val errMsg = errSb.toString()
          LOGGER.error(s"$logPrefix Async exception occured, possible reasons:\n$errMsg", ex)
          NotAcceptable(s"Errors:\n\n$errMsg")
        }
      }
    }
  }


  /** Реализация перехвата временных файлов сразу в MLocalImg-хранилище. */
  protected class LocalImgFileCreator(val mLocalImg: MLocalImg) extends TemporaryFileCreator { creator =>

    override def create(prefix: String, suffix: String): TemporaryFile = _create()

    override def create(path: Path): TemporaryFile = _create()

    private def _create(): TemporaryFile = {
      mLocalImgs.prepareWriteFile( mLocalImg )
      LocalImgFile
    }

    override def delete(file: TemporaryFile): Try[Boolean] = {
      Try {
        mLocalImgs.deleteAllSyncFor(mLocalImg.rowKey)
        true
      }
    }

    /** Маскировка MLocalImg под TemporaryFile. */
    object LocalImgFile extends TemporaryFile {

      private val _file = mLocalImgs.fileOf( mLocalImg )

      override def path: Path = _file.toPath
      override def file: File = _file
      override def temporaryFileCreator = creator
    }

  }

}

/** Контейнер результата работы BodyParser'а при аплоаде.
  * Вынесен за пределы контроллера из-за проблем с компиляцией routes, если это inner-class.
  */
protected class UploadBpRes(
                             val data     : MultipartFormData[TemporaryFile],
                             val localImg : Option[MLocalImg]
                           )


