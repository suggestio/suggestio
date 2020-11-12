package controllers

import java.io.{FileInputStream, RandomAccessFile}
import java.net.InetAddress
import java.time.OffsetDateTime

import javax.inject.Inject
import io.suggest.color.{MHistogram, MHistogramWs}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.crypto.hash.MHash
import io.suggest.ctx.MCtxId
import io.suggest.err.HttpResultingException, HttpResultingException._
import io.suggest.es.model.{EsModel, IMust, MEsNestedSearch}
import io.suggest.es.util.SioEsUtil
import io.suggest.file.MSrvFileInfo
import io.suggest.fio.{MDsRangeInfo, MDsReadArgs, MDsReadParams, WriteRequest}
import io.suggest.i18n.MMessage
import io.suggest.img.{MImgFormat, MImgFormats}
import io.suggest.n2.edge.edit.{MEdgeWithId, MNodeEdgeIdQs}
import io.suggest.n2.edge.{EdgeUid_t, MEdge, MEdgeDoc, MEdgeFlag, MEdgeFlagData, MEdgeFlags, MEdgeInfo, MNodeEdges, MPredicates}
import io.suggest.n2.edge.search.{Criteria, MHashCriteria}
import io.suggest.n2.media._
import io.suggest.n2.media.storage.{IMediaStorages, MStorageInfo}
import io.suggest.n2.node.{MNode, MNodeType, MNodes}
import io.suggest.n2.node.common.MNodeCommon
import io.suggest.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.pick.MimeConst
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.sec.av.{ClamAvScanRequest, ClamAvUtil}
import io.suggest.up.{MUploadChunkQs, MUploadResp, UploadConstants}
import io.suggest.url.MHostUrl
import io.suggest.ws.{MWsMsg, MWsMsgTypes}
import models.im._
import models.mup._
import models.req.IReq
import play.api.libs.json.Json
import play.api.mvc.{BodyParser, DefaultActionBuilder, MultipartFormData, Result, Results}
import play.core.parsers.Multipart
import util.acl.{BruteForceProtect, CanDownloadFile, CanUpload, Ctx304, IgnoreAuth, IsFileNotModified}
import util.cdn.{CdnUtil, CorsUtil}
import util.up.{FileUtil, UploadUtil}
import japgolly.univeq._
import monocle.Traversal
import util.img.detect.main.MainColorDetector
import util.ws.WsDispatcherActors
import io.suggest.ueq.UnivEqUtil._
import play.api.inject.Injector
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart

import scala.concurrent.{Future, blocking}
import scala.util.{Failure, Success}
import scalaz.ValidationNel
import scalaz.std.option._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.10.17 12:26
  * Description: Контроллер загрузки файлов на сервера s.io.
  */
final class Upload @Inject()(
                              sioControllerApi          : SioControllerApi,
                              injector                  : Injector,
                            )
  extends MacroLogsImpl
{

  import sioControllerApi._
  import mCommonDi.{ec, errorHandler}

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val uploadUtil = injector.instanceOf[UploadUtil]
  private lazy val canUpload = injector.instanceOf[CanUpload]
  private lazy val ignoreAuth = injector.instanceOf[IgnoreAuth]
  private lazy val cdnUtil = injector.instanceOf[CdnUtil]
  private lazy val iMediaStorages = injector.instanceOf[IMediaStorages]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val mImgs3 = injector.instanceOf[MImgs3]
  private lazy val mLocalImgs = injector.instanceOf[MLocalImgs]
  private lazy val clamAvUtil = injector.instanceOf[ClamAvUtil]
  private lazy val mainColorDetector = injector.instanceOf[MainColorDetector]
  private lazy val wsDispatcherActors = injector.instanceOf[WsDispatcherActors]
  private lazy val fileUtil = injector.instanceOf[FileUtil]
  private lazy val canDownloadFile = injector.instanceOf[CanDownloadFile]
  private lazy val bruteForceProtect = injector.instanceOf[BruteForceProtect]
  private lazy val isFileNotModified = injector.instanceOf[IsFileNotModified]
  private lazy val corsUtil = injector.instanceOf[CorsUtil]
  private lazy val defaultActionBuilder = injector.instanceOf[DefaultActionBuilder]
  private lazy val localImgFileCreatorFactory = injector.instanceOf[LocalImgFileCreatorFactory]


  // TODO Opt В будущем, особенно когда будет поддержка заливки видео (или иных больших файлов), надо будет
  // переписать body parser, чтобы возвращал просто Source[ByteString, _].
  // В качестве параллельных sink'ов надо сразу: swfs, clamav, hashesHex, colorDetector?, ffmpeg, etc... При ошибке - удалять из swfs.

  /** Body-parser для prepareUploadLogic. */
  def prepareUploadBp = parse.json[MFileMeta]

  /** Тело экшена подготовки к аплоаду.
    * Только тело, потому что ACL-проверки выносятся в основной контроллер, в контексте которого происходит загрузка.
    *
    * @param validated Провалидированные JSON-метаданные файла.
    * @param upInfo Контейнер данных, пробрасываемых целиком во вторую фазу.
    *
    * @return Created | Accepted | NotAcceptable  + JSON-body в формате MFileMeta.
    */
  def prepareUploadLogic(logPrefix          : String,
                         validated          : ValidationNel[String, MFileMeta],
                         upInfo             : MUploadInfoQs,
                        )(implicit request: IReq[MFileMeta]) : Future[Result] = {
    validated.fold(
      // Ошибка валидации присланных данных. Вернуть ошибку клиенту.
      {errorsNel =>
        LOGGER.warn(s"$logPrefix Failed to verify body: ${errorsNel.iterator.mkString(", ")}\n ${request.body}")
        val resp = MUploadResp(
          errors = errorsNel
            .iterator
            .map( MMessage(_) )
            .toSeq,
        )
        NotAcceptable( Json.toJson(resp) )
      },

      // Успешно провалидированы данные файла для загрузки.
      {fileMeta =>
        import esModel.api._
        LOGGER.trace(s"$logPrefix Body validated, user#${request.user.personIdOpt.orNull}:\n $fileMeta")

        for {
          // Поискать файл с такими параметрами в MMedia:
          fileSearchRes <- mNodes.dynSearch(
            new MNodeSearch {
              // Тут по предикату - надо ли фильтровать?
              override val outEdges: MEsNestedSearch[Criteria] = {
                val cr = Criteria(
                  fileSizeB = fileMeta.sizeB.get :: Nil,
                  fileHashesHex = (for {
                    fmHash <- fileMeta.hashesHex.iterator
                  } yield {
                    MHashCriteria(
                      hTypes    = fmHash.hType :: Nil,
                      hexValues = fmHash.hexValue :: Nil,
                      must      = IMust.MUST,
                    )
                  })
                    .toSeq,
                )
                MEsNestedSearch(
                  clauses = cr :: Nil,
                )
              }
              override def limit = 1
            }
          )

          foundFileNodeOpt = {
            LOGGER.trace(s"$logPrefix Found ${fileSearchRes.length} node by source hashes: ${fileSearchRes.iterator.flatMap(_.id).mkString(", ")}")
            fileSearchRes.headOption
          }
          fileEdgeOpt = foundFileNodeOpt
            .iterator
            .flatMap( _.edges.withPredicateIter( MPredicates.Blob.File ) )
            .nextOption()

          (respStatus, respDataFut) = (for {
            fileEdge <- fileEdgeOpt
            foundFileNode <- foundFileNodeOpt
            inProgressFlag = MEdgeFlags.InProgress

            // Если есть флаг InProgress, то надо возвращать uploadUrls, несмотря на присутствие узла.
            if {
              val isInProgress = fileEdge.info.flags
                .exists( _.flag ==* inProgressFlag )
              if (isInProgress)
                LOGGER.debug(s"$logPrefix File node#${foundFileNode.idOrNull} have $inProgressFlag in edge. isEnabled=${foundFileNode.common.isEnabled}\n FileEdge=$fileEdge")
              !isInProgress
            }

          } yield {
            // Найден узел с уже закачанным файлом. Собрать необходимые данные и вернуть клиенту.
            val fileEdge = foundFileNode
              .edges
              .withPredicateIter( MPredicates.Blob.File )
              .next()

            val edgeMedia = fileEdge.media.get

            val existColors = edgeMedia
              .picture
              .histogram
              .colorsOrNil

            LOGGER.debug(s"$logPrefix Found existing file media#${foundFileNode.idOrNull} for hashes [${fileMeta.hashesHex.iterator.map(_.hexValue).mkString(", ")}] with ${existColors.size} colors.")

            // Тут шаринг инстанса возможной картинки. Но надо быть осторожнее: если не картинка, то может быть экзепшен.
            lazy val foundFileImg = MImg3.fromEdge( foundFileNode.id.get, fileEdge ).get

            // Собираем гистограмму цветов.
            val mediaColorsFut = if (
              existColors.isEmpty &&
                !edgeMedia.file.isOriginal &&
                edgeMedia.picture.nonEmpty
            ) {
              // Для получения гистограммы цветов надо получить на руки оригинал картинки.
              val origImg3 = foundFileImg.original
              for {
                origMediaOpt <- mNodes.getByIdCache( origImg3.dynImgId.mediaId )
              } yield {
                // TODO Если не найдено оригинала, то может быть сразу ошибку? Потому что это будет нечто неюзабельное.
                if (origMediaOpt.isEmpty)
                  LOGGER.warn(s"$logPrefix Orig.img $mImgs3 not found media#${origImg3.mediaId}, but derivative media#${foundFileNode.idOrNull} is here: $foundFileNode")

                // Поиск гистограммы цветов в узле оригинала:
                (for {
                  origMedia <- origMediaOpt.iterator
                  e <- origMedia.edges.withPredicateIter( MPredicates.Blob.File )
                  edgeMedia <- e.media
                  histogram <- edgeMedia.picture.histogram
                  if histogram.colors.nonEmpty
                } yield {
                  histogram
                })
                  .nextOption()
                  .colorsOrNil
              }
            } else {
              LOGGER.trace(s"$logPrefix Existing color histogram for media#${foundFileNode.idOrNull}: [${existColors.iterator.map(_.hexCode).mkString(", ")}]")
              Future.successful( existColors )
            }

            // Собрать ответ с помощью награбленных цветов.
            val upRespFut = for (mediaColors <- mediaColorsFut) yield {
              MUploadResp(
                fileExist = Some(MSrvFileInfo(
                  nodeId  = foundFileNode.id.get,
                  // TODO Сгенерить ссылку на файл. Если это картинка, то через dynImgArgs
                  url = if (edgeMedia.file.imgFormatOpt.nonEmpty) {
                    // TODO IMG_DIST: Вписать хост расположения картинки.
                    // TODO Нужна ссылка картинки на недо-оригинал картинки? Или как?
                    Some( routes.Img.dynImg( foundFileImg ).url )
                  } else {
                    // TODO IMG_DIST Надо просто универсальную ссылку для скачивания файла, независимо от его типа.
                    LOGGER.error(s"$logPrefix MIME ${edgeMedia.file.mime getOrElse ""} don't know how to build URL")
                    None
                  },
                  // TODO foundFile.file.dateCreated - не раскрывать. Лучше удалить MFileMeta.dateCreated после переезда в MEdge.media
                  fileMeta = edgeMedia.file,
                  pictureMeta = MPictureMeta(
                    // TODO !!! Выгребать из оригинала картинки, а не из любой найденной по хешам.
                    histogram = OptionUtil.maybe( mediaColors.nonEmpty ) {
                      MHistogram(
                        colors = mediaColors
                          .sortBy { p =>
                            p.freqPc.fold(0)(-_)
                          }
                          .toList
                      )
                    }
                  ),
                  storage = OptionUtil.maybeOpt( upInfo.systemResp contains true )(edgeMedia.storage),
                )),
              )
            }
            (Accepted, upRespFut)

          }).getOrElse {
            val foundFileNodeIdOpt = foundFileNodeOpt.flatMap(_.id)

            // Выставить в QS existNodeId, если найден подходящий узел неоконченной закачки.
            val upDataFut = for {
              resOpt <- FutureUtil.optFut2futOptPlain( for {
                foundFileNodeId <- foundFileNodeIdOpt
                // existNodeId не выставлен, т.к. он будет перезаписан ниже.
                if upInfo.existNodeId.isEmpty
                fileEdge <- fileEdgeOpt
                inProgressFlag = MEdgeFlags.InProgress
                if fileEdge.info.flags.exists(_.flag ==* inProgressFlag)
                fileEdgeMedia <- fileEdge.media
                fileStorage <- fileEdgeMedia.storage
              } yield {
                // Если найден file-узел, но с флагом InProgress, надо продолжить заливку в данный узел.
                LOGGER.debug( s"$logPrefix Patching Upload qsInfo.existNodeId := $foundFileNodeId -- file-edge has $inProgressFlag flag, recovering upload." )
                for {
                  storAssigned <- cdnUtil.toAssignedStorage( foundFileNodeId, fileStorage )
                } yield {
                  val upInfo22 = (MUploadInfoQs.existNodeId set foundFileNodeIdOpt)( upInfo )
                  upInfo22 -> storAssigned
                }
              })

              // Если найден подходящий узел неоконченного upload, то надо попытаться собрать данные по хранилищу для него.
              (upInfo9, assignResp) <- (for {
                (upInfo22, storAssignedOpt) <- resOpt
                storAssigned <- storAssignedOpt
              } yield {
                LOGGER.trace(s"$logPrefix User will upload to already assigned storage: $storAssigned")
                Future.successful( upInfo22 -> storAssigned )
              }) getOrElse {
                // Нет даже недокачанного узла нет. Создать новый: чтобы chunked-заливка заработала, надо создать новый.
                for (existNodeId <- upInfo.existNodeId)
                  LOGGER.warn( s"$logPrefix Found InProgress Upload file node#${foundFileNodeIdOpt.orNull}, but qsInfo.existNodeId=$existNodeId" )

                for {
                  assignResp        <- cdnUtil.assignDist( fileMeta )
                  // Взять будущий инстанс FileCreator'а, чтобы узнать возможный dynImgId (если у нас картинка).
                  fileCreator       = _getFileCreator( upInfo.fileHandler, fileMeta )
                  fileEdge          = _mkFileEdge(
                    fileMeta    = fileMeta,
                    storageInfo = assignResp.storage,
                    edgeFlags   = MEdgeFlags.InProgress :: Nil,
                    nodeIds     = assignResp.host.allHostNames,
                  )
                  // Сохраняем новый файловый узел.
                  mnode <- upInfo.existNodeId.fold {
                    _createFileNode(
                      fileCreator = fileCreator,
                      nodeType    = upInfo.nodeType.get,
                      techName    = None,
                      edges       = fileEdge :: _mkCreatedByEdge( request.user.personIdOpt ).toList,
                      enabled     = false,
                    )
                  } { existNodeId =>
                    for {
                      existNodeOpt0 <- mNodes.getByIdCache( existNodeId )
                      existNode0 = existNodeOpt0.get

                      // Нужно удалить старый file-эдж вместе с данными в хранилище.
                      _ <- Future.sequence(for {
                        fileEdge0 <- existNode0.edges.withPredicateIter( MPredicates.Blob.File )
                        edgeMedia <- fileEdge0.media
                        stor <- edgeMedia.storage
                        storClient = {
                          LOGGER.debug(s"$logPrefix Will erase $stor from node#$existNodeId before uploading new file.")
                          iMediaStorages.client( stor.storage )
                        }
                      } yield {
                        storClient
                          .delete( stor.data )
                          .andThen {
                            case Success(r)  => LOGGER.info(s"$logPrefix Erased $stor from node#$existNodeId => r")
                            case Failure(ex) => LOGGER.warn(s"$logPrefix Failed to erase $stor from node#$existNodeId", ex)
                          }
                      })

                      // Теперь, обновить текущий узел, удалив старый file-эдж и добавив новый:
                      existNode2 <- mNodes.tryUpdate( existNode0 )(
                        MNode.node_meta_basic_dateEdited_RESET andThen
                        MNode.edges.modify { edges0 =>
                          val edgesSeq2 = MNodeEdges.edgesToMap1(
                            edges0.withoutPredicateIter( MPredicates.Blob ) ++ (fileEdge :: Nil)
                          )
                          (MNodeEdges.out set edgesSeq2)(edges0)
                        } andThen
                        MNode.common
                          .composeLens( MNodeCommon.isEnabled )
                          .set( false )
                      )
                    } yield {
                      LOGGER.info(s"$logPrefix Updated node#$existNodeId v=${existNode2.versionOpt.orNull} for chunked uploading")
                      existNode2
                    }
                  }
                } yield {
                  LOGGER.debug(s"$logPrefix For $fileCreator created node#${mnode.id.orNull} w/o techName")
                  // Вернуть ожидаемый ответ:
                  val upInfoNoded = (MUploadInfoQs.existNodeId set mnode.id)( upInfo )
                  upInfoNoded -> assignResp
                }
              }

            } yield {
              LOGGER.trace(s"$logPrefix Assigned swfs resp: $assignResp")

              val upData = MUploadTargetQs(
                fileProps   = fileMeta,
                personId    = request.user.personIdOpt,
                validTillS  = uploadUtil.ttlFromNow(),
                // TODO Узел теперь создаётся всегда. Надо ли рендерить assign-данные целиком? Может host-url достаточно?
                storage     = assignResp,
                info        = upInfo9,
              )

              // Список хостнеймов: в будущем возможно, что ссылок для заливки будет несколько: основная и запасная. Или ещё что-то.
              val hostnames =
                assignResp.host.namePublic ::
                  // TODO Вписать запасные хостнеймы для аплоада?
                  Nil

              val relUrl = routes.Upload.doFileUpload(upData).url
              MUploadResp(
                // IMG_DIST: URL включает в себя адрес ноды, на которую заливать.
                upUrls = for (host <- hostnames) yield {
                  // TODO В будущем нужно возвращать только хост и аргументы, а клиент пусть сам через js-роутер ссылку собирает.
                  // TODO Для этого нужно MUploadTargetQs сделать JSON-моделью с отдельным полем сигнатуры.
                  MHostUrl(
                    host = host,
                    relUrl = relUrl
                  )
                }
              )
            }

            (Created, upDataFut)
          }

          respData <- respDataFut
        } yield {
          respStatus( Json.toJson(respData) )
        }
      }
    )
  }


  /** Сборка инстанса для сохранения/доступа в модель LocalImg. */
  private def _localImgFileCreator(imgFormatOpt: Option[MImgFormat]): LocalImgFileCreator = {
    val dynImgId = MDynImgId(
      origNodeId = MDynImgId.randomId(),
      imgFormat  = imgFormatOpt,
    )
    val localImg = MLocalImg( dynImgId )
    val args = MLocalImgFileCreatorArgs( localImg )
    localImgFileCreatorFactory.instance( args )
  }


  /** Получить инстанс fileCreator из qs-аргументов. */
  private def _getFileCreator(fileHandlerOpt: Option[MUploadFileHandler],
                              fileMeta: MFileMeta): LocalImgFileCreator = {
    // Используем всегда модель MLocalImg, т.к. штатный SingletonTemporaryFileCreator сохраняет tmp-файлы в /tmp
    // или куда попало, что не редко означает сохранение в tmpfs (в RAM), что небезопасно.

    // Если картинка, то заливать сразу в MLocalImg/ORIG.
    val imgFmtOpt = fileMeta.mime
      .flatMap( MImgFormats.withMime )

    fileHandlerOpt.foreach {
      case MUploadFileHandlers.Image =>
        // Если указано, что ожидается картинка, то убедится в картиночности заявленного файла.
        if (imgFmtOpt.isEmpty)
          throw new NoSuchElementException(s"Non-image or unsupported MIME type: ${fileMeta.mime}")
    }

    _localImgFileCreator( imgFmtOpt )
  }


  /** BodyParser для запросов, содержащих файлы для аплоада. */
  private def _uploadFileBp(uploadArgs: MUploadTargetQs): BodyParser[MUploadBpRes] = {
    // Сборка самого BodyParser'а.
    parse.using { rh =>
      if (rh.hasBody) {
        val fileCreator = _getFileCreator( uploadArgs.info.fileHandler, uploadArgs.fileProps )
        // Есть тело запроса - принимаем файл.
        for {
          mpfd <- parse.multipartFormData(
            Multipart.handleFilePartAsTemporaryFile( fileCreator ),
            maxLength = uploadArgs.fileProps.sizeB.get + 10000L,
          )
        } yield {
          // Завернуть итог парсинга в контейнер, содержащий данные MLocalImg или иные возможные поля.
          MUploadBpRes(
            data = mpfd,
            fileCreator = fileCreator,
          )
        }
      } else {
        // Нет тела - нет дела. Ищем принятый chunk'ед файл через MLocalImg.
        val imgFmtOpt = uploadArgs.fileProps.mime
          .flatMap( MImgFormats.withMime )

        val localImg = MLocalImg( MDynImgId(uploadArgs.info.existNodeId.get, imgFmtOpt) )
        val locImgFile = mLocalImgs.fileOf( localImg )

        if ( !locImgFile.exists() ) {
          LOGGER.warn(s"bp.!body: Not found local file: $locImgFile\n args = $uploadArgs")
          val respFut = errorHandler.onClientError(rh, BAD_REQUEST, s"File not uploaded: ${uploadArgs.info.existNodeId getOrElse ""}")
          parse.error( respFut )

        } else {
          LOGGER.trace(s"Found pre-uploaded file: $locImgFile")
          val lifc = localImgFileCreatorFactory.instance( MLocalImgFileCreatorArgs( localImg ) )
          parse.ignore(
            MUploadBpRes(
              data = MultipartFormData(
                files = FilePart[TemporaryFile](
                  key = UploadConstants.MPART_FILE_FN,
                  // TODO Для chunk оно сохраняется в узле, доступ к которому из bodyParser'а не доступен.
                  filename = "",
                  contentType = uploadArgs.fileProps.mime,
                  ref = LocalImgFile(
                    file    = locImgFile,
                    creator = lifc,
                  ),
                  fileSize = locImgFile.length(),
                ) :: Nil,
                dataParts = Map.empty,
                badParts  = Nil,
              ),
              fileCreator = lifc,
            )
          )
        }
      }
    }
  }


  /** Экшен фактического аплоада файла с клиента.
    * Файл приходит как часть multipart.
    *
    * @param uploadArgs Подписанные данные аплоада
    * @param ctxIdOpt Подписанный (но пока непроверенный) ctxId.
    * @return Ok + MUploadResp | NotAcceptable
    */
  def doFileUpload(uploadArgs: MUploadTargetQs, ctxIdOpt: Option[MCtxId]) = {
    // CSRF выключен, потому что session cookie не пробрасывается на ноды.

    // !!! Этот метод часто вызывается дважды на один запрос, если файл длинный или канал медленный.
    // !!! https://github.com/playframework/playframework/issues/8185
    val uploadNow = uploadUtil.rightNow()
    lazy val logPrefix = s"doFileUpload()#${uploadNow.toMillis}:"

    if ( !uploadUtil.isTtlValid( uploadArgs.validTillS, now = uploadNow ) ) {
      // TTL upload-ссылки истёк. Огорчить юзера.
      defaultActionBuilder.async( parse.ignore(null: MUploadBpRes) ) { implicit request =>
        val msg = "URL TTL expired"
        LOGGER.warn(s"$logPrefix $msg: ${uploadArgs.validTillS}; now was == ${uploadNow.toSeconds}")
        errorHandler.onClientError( request, play.api.http.Status.NOT_ACCEPTABLE, msg )
      }

    } else canUpload.file(uploadArgs, ctxIdOpt).async( _uploadFileBp(uploadArgs) ) { implicit request =>
      LOGGER.trace(s"$logPrefix Starting w/args: $uploadArgs")

      lazy val errSb = new StringBuilder()
      def __appendErr(msg: String): Unit = {
        LOGGER.warn(s"$logPrefix $msg")
        errSb.append(msg).append('\n')
      }

      // Начинаем с синхронных проверок:
      val resFutOpt: Option[Future[Result]] = for {

        // Поискать файл в теле запроса:
        filePart <- {
          val partName = UploadConstants.MPART_FILE_FN
          val fpOpt = request.body.data.file( partName )
          LOGGER.trace(s"$logPrefix File part '$partName' lookup: found?${fpOpt.nonEmpty}: ${fpOpt.orNull}")
          if (fpOpt.isEmpty)
            __appendErr(s"Missing file part with name '$partName'")
          fpOpt
        }

        // Проверить Content-Type, заявленный в теле запроса:
        if {
          val r = filePart.contentType
            .exists(_ equalsIgnoreCase uploadArgs.fileProps.mime.get)
          LOGGER.trace(s"$logPrefix Content-type verify: expected=${uploadArgs.fileProps.mime.get} filePart=${filePart.contentType} ;; match?$r")
          if (!r)
            __appendErr( s"Expected Content-type '${uploadArgs.fileProps.mime.get}' not matching to Multipart part's content-type '${filePart.contentType.orNull}'." )
          r
        }

        // Сборка Upload-контекста. Дальнейший сбор информации по загруженному файлу должен происходить в контексте.
        upCtx = uploadUtil.makeUploadCtx( filePart.ref.path, uploadArgs.info.fileHandler )

        sizeB = uploadArgs.fileProps.sizeB.get
        // Сверить размер файла с заявленным размером
        if {
          val srcLen = upCtx.fileLength
          val r = (srcLen ==* sizeB)
          LOGGER.trace(s"$logPrefix File size check: expected=$sizeB detected=$srcLen ;; match? $r")
          if (!r)
            __appendErr(s"Size of file $srcLen bytes, but expected is $sizeB bytes.")
          r
        }

        declaredMime = uploadArgs.fileProps.mime.get

        // Определить MIME-тип принятого файла:
        detectedMimeType <- upCtx.detectedMimeTypeOpt
        // Бывает, что MIME не совпадает. Решаем, что нужно делать согласно настройкам аплоада. Пример:
        // Detected file MIME type [application/zip] does not match to expected [application/vnd.android.package-archive]
        mimeType = {
          if (
            (detectedMimeType ==* declaredMime) ||
            // игнорить octet-stream, т.к. это означает, что MIME неизвестен на клиенте.
            (declaredMime ==* MimeConst.APPLICATION_OCTET_STREAM)
          ) {
            detectedMimeType
          } else {
            val msg = s"Detected file MIME type [${upCtx.detectedMimeTypeOpt.orNull}] does not match to expected $declaredMime"
            LOGGER.warn(s"$logPrefix $msg")
            uploadArgs.info.obeyMime.fold {
              __appendErr( msg )
              throw new NoSuchElementException(msg)
            } {
              case false =>
                LOGGER.info(s"$logPrefix Client-side MIME [$declaredMime] replaced with server-side detected MIME: [$detectedMimeType]")
                detectedMimeType
              case true =>
                LOGGER.info(s"$logPrefix Client-side MIME [$declaredMime] will be saved, instead of detected MIME [$detectedMimeType]")
                declaredMime
            }
          }
        }

        // Прежде чем запускать тяжелые проверки, надо быстро сверить лимиты для текущего типа файла:
        if {
          val r = upCtx.validateFileContentEarly()
          if (!r)
            __appendErr( s"Failed to validate size limits: len=${upCtx.fileLength}b img=${request.body.fileCreator.liArgs.mLocalImg.dynImgId.imgFormat.orNull}/${upCtx.imageWh.orNull}" )
          r
        }

      } yield {
        import esModel.api._
        val startMs = System.currentTimeMillis()
        // Лёгкие синхронные проверки завершены успешно. Переходим в асинхрон и в тяжелые проверки.

        // В фоне запустить JVM-only валидацию содержимого файла. Все файлы должны иметь корректный внутренний формат.
        val isFileValidFut = upCtx.validateFileFut()

        for {
          // Рассчитать хэш-суммы файла (digest).
          hashesHex2 <- fileUtil.mkHashesHexAsync(
            file    = upCtx.file,
            hashes  = uploadArgs.fileProps
              .hashesHex
              .map(_.hType),
            flags   = MFileMetaHashFlags.ORIGINAL_FLAGS,
          )

          hashesHexMap = hashesHex2
            .zipWithIdIter[MHash]
            .to( Map )

          // Сверить рассчётные хэш-суммы с заявленными при загрузке.
          if {
            val isHashesMatching =
              hashesHexMap.nonEmpty &&
              uploadArgs
                .fileProps
                .hashesHex
                .forall { fmHashQs =>
                  hashesHexMap
                    .get( fmHashQs.hType )
                    .exists { mfhash =>
                      val r = mfhash.hexValue ==* fmHashQs.hexValue
                      if (!r) {
                        LOGGER.error(s"$logPrefix $mfhash != ${fmHashQs.hType} ${fmHashQs.hexValue} file=${upCtx.file} ${upCtx.fileLength}b user=${request.user.personIdOpt.orNull}")
                        errSb.synchronized {
                          __appendErr( s"File hash ${fmHashQs.hType.fullStdName} '${mfhash.hexValue}' doesn't match to declared '${fmHashQs.hexValue}'." )
                        }
                      }
                      r
                    }
                }

            // Залоггировать, иначе в trace-логах совсем ничего не печатается на тему логов.
            if (isHashesMatching)
              LOGGER.trace(s"$logPrefix ${hashesHexMap.size}/${uploadArgs.fileProps.hashesHex.size} hashes validated OK\n ${_renderHashesHex( hashesHexMap.valuesIterator )}")
            else {
              LOGGER.warn(s"$logPrefix File hashes does not match.\n Declared in qs = ${_renderHashesHex( uploadArgs.fileProps.hashesHex )}\n Calculated = ${_renderHashesHex( hashesHexMap.valuesIterator )}")
            }

            isHashesMatching
          }

          // Убедиться, что формат файла валиден.
          isFileValid <- isFileValidFut
          if isFileValid

          // Финальное тестирование внутренностей: перед возможным вызовом всяких imagemagick или иной утили, убедиться,
          // что в файле не содержится вредоносного кода.
          // Анти-оптимизация: Запускаем ТОЛЬКО ПОСЛЕ pure-java-проверок, т.к. в clam тоже находят дыры. https://www.opennet.ru/opennews/art.shtml?num=47964
          clamAvScanResFut = clamAvUtil.scan(
            ClamAvScanRequest(
              file = upCtx.file.getAbsolutePath
            )
          )

          clamAvScanRes <- clamAvScanResFut
          if {
            val r = clamAvScanRes.isClean
            LOGGER.trace(s"ClamAV: clean?$r took=${System.currentTimeMillis() - startMs} ms. ret=$clamAvScanRes")
            if (!r) {
              LOGGER.warn(s"ClamAV INFECTED $clamAvScanRes for file ${upCtx.path}. See logs upper.\n User session: ${request.user.personIdOpt.orNull}\n remote: ${request.remoteClientAddress}\n User-Agent: ${request.headers.get(USER_AGENT).orNull}")
              errSb.synchronized {
                __appendErr( s"AntiVirus check failed." )
              }
            }
            r
          }

          // Имя файла. Может быть задано в узле, а в body - отсутствовать (""), если chunked upload.
          fileNameOpt = Option( filePart.filename ).filter(_.nonEmpty)

          // Собираем MediaStorage ptr и клиент:
          storageInfo = uploadArgs.storage.storage
          storageClient = iMediaStorages.client( storageInfo.storage )

          // TODO Если требуется img-форматом, причесать оригинал, расширив исходную карту хэшей новыми значениями.
          // Например, JPEG можно пропустить через jpegtran -copy.

          // Запускаем в фоне заливку файла из ФС в надёжное распределённое хранилище:
          saveFileToShardFut = {
            val wr = WriteRequest(
              contentType  = mimeType,
              file         = upCtx.file,
              origFileName = fileNameOpt
            )
            LOGGER.trace(s"$logPrefix Will save file $wr to storage $storageInfo ...")
            storageClient.write( storageInfo.data, wr )
          }

          fileMeta = MFileMeta(
            mime       = Some( mimeType ),
            sizeB      = uploadArgs.fileProps.sizeB,
            isOriginal = true,
            hashesHex  = hashesHex2,
          )

          fileEdge = _mkFileEdge(
            fileMeta,
            storageInfo,
            pictureMeta = MPictureMeta(
              whPx = upCtx.imageWh,
            ),
            nodeIds = Set.empty + uploadUtil.MY_NODE_PUBLIC_HOST,
          )

          // Проверки закончены. Пора переходить к действиям по сохранению узла, сохранению и анализу файла.
          mnode1Fut = {
            // Записываем id юзера, который первым загрузил этот файл.
            val createdByOpt = _mkCreatedByEdge(
              request.user.personIdOpt,
              uploadArgs.personId,
            )
            val addEdges = fileEdge :: createdByOpt.toList

            // Собрать/обновить и сохранить узел в БД.
            val fut = request.existNodeOpt.fold [Future[MNode]] {
              val ntype = uploadArgs.info.nodeType.get
              LOGGER.trace(s"$logPrefix qs.existNodeId empty => Creating new node of type#$ntype for $storageInfo")
              // Создание нового узла под файл.
              _createFileNode(
                fileCreator = request.body.fileCreator,
                nodeType = ntype,
                techName = fileNameOpt,
                edges = addEdges,
                enabled = true,
              )

            } { mnode0 =>
              // Полулегальный патчинг эджей существующего файлового узла новым файлом.
              // Удаление старого файла будет после сохранения узла.
              LOGGER.trace(s"$logPrefix Will update existing node#${mnode0.idOrNull} with edges: ${addEdges.mkString(", ")}")

              var modF = (
                MNode.node_meta_basic_dateEdited_RESET andThen
                MNode.edges.modify { edges0 =>
                  val edgesSeq2 = MNodeEdges.edgesToMap1(
                    edges0.withoutPredicateIter( MPredicates.CreatedBy, MPredicates.Blob ) ++ addEdges
                  )
                  MNodeEdges.out.set( edgesSeq2 )(edges0)
                }
              )

              if (!mnode0.common.isEnabled) {
                LOGGER.trace(s"$logPrefix Reset node#${mnode0.idOrNull} isEnabled => true")
                modF = modF andThen MNode.common
                  .composeLens( MNodeCommon.isEnabled )
                  .set(true)
              }

              mNodes.tryUpdate( mnode0 )(modF)
                // Версия потом иногда отправляется в system-extra, поэтому инкрементим её:
                .map( MNode.versionOpt.modify(_.map(_ + 1)) )
            }

            // При ошибке сохранения узла надо удалить файл из saveFileToShardFut
            for {
              _ <- fut.failed
              _ <- {
                LOGGER.error(s"$logPrefix Failed to save MNode. Deleting file storage#$storageInfo...")
                saveFileToShardFut
              }
              delFileRes <- storageClient.delete( storageInfo.data )
            } {
              LOGGER.warn(s"$logPrefix Emergency deleted ok file#$storageInfo => $delFileRes")
            }

            fut
          }

          // Сразу в фоне запускаем анализ цветов картинки, если он был запрошен.
          // Очень маловероятно, что сохранение сломается и будет ошибка, поэтому и параллелимся со спокойной душой.
          colorDetectFutOpt = for {
            cdArgs          <- uploadArgs.info.colorDetect
          } yield {
            val localImg = request.body.fileCreator.liArgs.mLocalImg
            mainColorDetector.cached( localImg ) {
              mainColorDetector.detectPaletteFor( localImg, maxColors = cdArgs.paletteSize)
            }
          }

          // Ожидаем окончания сохранения узла.
          mnode1 <- mnode1Fut
          _ = Future( mNodes.putToCache( mnode1 ) )

          // Файл сохранён. Но если был патчинг уже существующего узла, то надо убрать старый файл из хранилища.
          // Поискать старый файловый эдж для удаления.
          _ = {
            request
              .existNodeOpt
              .fold [Future[_]] ( Future.successful(()) ) { mnode0 =>
                val fut = for {
                  files4deleteOpts <- Future.traverse(
                    // В старом узле, в норме, всегда 0 или 1 файловый эдж, поэтому оптимизировать ничего не требуется:
                    mnode0.edges
                      .withPredicateIter( MPredicates.Blob.File )
                      .to( List )
                  ) { oldFileEdge =>
                    fileUtil.isNeedDeleteFile( oldFileEdge, mnode1, reportDupEdge = true )
                  }
                  files4delete = files4deleteOpts.flatten
                  _ <- {
                    if (files4delete.nonEmpty) {
                      val files4deleteLen = files4delete.length
                      LOGGER.info(s"$logPrefix Deleting $files4deleteLen files after patching node#${mnode0.idOrNull}:\n ${files4delete.mkString("\n ")}")
                      val delFut = Future.traverse( files4delete )( fileUtil.deleteFile )
                      for (_ <- delFut)
                        LOGGER.debug(s"$logPrefix Successfully erased $files4deleteLen previously existed-files of node#${mnode0.idOrNull}.")
                      delFut
                    } else {
                      Future.successful(None)
                    }
                  }
                } yield {
                  None
                }

                for (ex <- fut.failed)
                  LOGGER.error(s"$logPrefix Failed to delete stored files of node#${mnode0.idOrNull}", ex)

                fut
              }
          }

          mnodeId = mnode1.id.get

          // Потом в фоне вне основного экшена сохранить результат детектирования основных цветов картинки в MMedia.PictureMeta:
          _ = {
            for (colorDetectFut <- colorDetectFutOpt) {
              val saveColorsFut = for (colorHist <- colorDetectFut) yield {
                if (colorHist.colors.nonEmpty) {
                  // Считаем общее кол-во пикселей для нормировки частот цветов:
                  val colorsHist2 = colorHist.withRelFrequences
                  lazy val colorsCount = colorsHist2.colors.size

                  LOGGER.trace(s"$logPrefix Detected $colorsCount top-colors on node#$mnodeId:\n ${colorsHist2.colors.iterator.map(_.hexCode).mkString(", ")}")

                  val mmedia2OptFut = mNodes.tryUpdate(mnode1)(
                    MNode.edges.modify { edges0 =>
                      val pred = MPredicates.Blob.File
                      val fileEdges1 = MNodeEdges.edgesToMap1(
                        edges0
                          .withPredicateIter( pred )
                          .map(
                            MEdge.media
                              .composeTraversal( Traversal.fromTraverse[Option, MEdgeMedia] )
                              .composeLens( MEdgeMedia.picture )
                              .composeLens( MPictureMeta.histogram )
                              .set( Some(colorsHist2) )
                          )
                      )
                      val edges2 = MNodeEdges.edgesToMap1(
                        edges0.withoutPredicateIter(pred) ++ fileEdges1
                      )
                      MNodeEdges.out.set( edges2 )(edges0)
                    }
                  )

                  mmedia2OptFut.onComplete {
                    case Success(res) =>
                      LOGGER.debug(s"$logPrefix Updated MNode#$mnodeId with $colorsCount main colors, v=${res.versionOpt.orNull}.")
                    case Failure(ex) =>
                      LOGGER.error(s"$logPrefix Failed to update MMedia#$mnodeId with main colors", ex)
                  }

                } else {
                  LOGGER.warn(s"$logPrefix Color detector returned NO colors o_O")
                }
              }

              // Логгировать ошибки:
              for (cdEx <- saveColorsFut.failed) {
                LOGGER.error(s"$logPrefix Detect/save main colors failure", cdEx)
              }
            }
          }

          // Дожидаемся окончания заливки файла в хранилище
          saveFileToShardRes <- {
            // При ошибке заливки файла, надо удалять созданный узел и запись в MMedia:
            for {
              ex <- saveFileToShardFut.failed

              delNodeResFut = {
                val needDeleteNode = uploadArgs.info.existNodeId.isEmpty
                LOGGER.error(s"$logPrefix Failed to send file into storage#$storageInfo . Delete node#$mnodeId?${needDeleteNode}", ex)
                if (needDeleteNode)
                  mNodes.deleteById( mnodeId )
                else
                  Future.successful( false )
              }

              // Запустить в фоне чистку хранилища, на случай если там что-то всё-таки осело, несмотря на ошибку.
              _ = storageClient
                .delete( storageInfo.data )
                .onComplete { tryRes =>
                  LOGGER.debug(s"$logPrefix Storage clean-up after error => $tryRes")
                }

              delNodeRes <- delNodeResFut
            } {
              if (delNodeRes)
                LOGGER.warn(s"$logPrefix Emergency deleted MNode#$mnodeId => $delNodeRes")
            }

            saveFileToShardFut
          }

        } yield {
          // Процедура проверки и сохранения аплоада завершёна успешно!
          LOGGER.info(s"$logPrefix File storage: saved ok. r => $saveFileToShardRes")

          // Пытаемся синхронно получить цвета из асихронного фьючерса, чтобы доставить их клиенту наиболее
          // оптимальным путём и снижая race-condition между WebSocket и http-ответом этого экшена:
          val colorsOpt: Option[MHistogram] = for {
            colorDetectFut      <- colorDetectFutOpt
            syncCurrentValueTry <- colorDetectFut.value
            syncCurrentValue    <- syncCurrentValueTry.toOption
          } yield {
            syncCurrentValue
          }

          // Вернуть 200 Ok с данными по файлу
          val isSystemResp = uploadArgs.info.systemResp contains[Boolean] true
          val resp = MUploadResp(
            fileExist = Some( MSrvFileInfo(
              nodeId = mnodeId,
              // TODO url: Надо бы вернуть ссылку. Для img - routes.Img.*, для других - по другим адресам.
              // Нет необходимости слать это всё назад, поэтому во всех заведомо известных клиенту поля None или empty:
              pictureMeta = MPictureMeta(
                histogram = colorsOpt,
              ),
              storage = Option.when( isSystemResp )(storageInfo),
              // На клиенте есть такой же fileMeta, но всё же возвращаем его в ответе. И это крайне желательно для аплоада через SysNodeEdges.
              fileMeta = fileMeta,
            )),
            extra = Option.when( isSystemResp ) {
              // Для sys-запросов: В extra-данных надо передать сериализованный обновлённый эдж и обновлённую координату эджа.
              val edgeExtra = MEdgeWithId(
                edgeId = MNodeEdgeIdQs(
                  nodeId  = mnode1.id.get,
                  nodeVsn = mnode1.versionOpt.get,
                  edgeId = Some {
                    mnode1.edges.out
                      .zipWithIndex
                      .find(_._1 ===* fileEdge)
                      .get
                      ._2
                  },
                ),
                edge = fileEdge,
              )
              Json
                .toJson( edgeExtra )
                .toString()
            },
          )
          val respJson = Json.toJson(resp)

          // side-effect: Если оказалось, что MainColorDetector не успел всё сделать к текущему моменту (это нормально),
          // то запустить передачу данных от него по websocket'у:
          // Отправляем как можно ПОЗЖЕ, чтобы максимально снизить race conditions между веб-сокетом и результатом экшена.
          if (colorsOpt.isEmpty) {
            LOGGER.trace(s"$logPrefix ColorDetector not completed yet. Connect it with websocket.")
            for {
              colorDetectFut <- colorDetectFutOpt
              cdArgs         <- uploadArgs.info.colorDetect
              ctxId          <- ctxIdOpt
              hasTransparentColorFut <- upCtx.imageHasTransparentColors()
            } {
              LOGGER.trace(s"$logPrefix ColorDetect+WS: for uploaded image, ctxId#${ctxId.key}")
              val wsNotifyFut = for {
                mhist0 <- colorDetectFut
                hasTransparentColor <- hasTransparentColorFut
              } yield {
                val mhist2 = mhist0.shrinkColorsCount( cdArgs.wsPaletteSize )
                val wsMsg = MWsMsg(
                  typ     = MWsMsgTypes.ColorsHistogram,
                  payload = Json.toJson {
                    MHistogramWs(
                      nodeId          = mnodeId,
                      hist            = mhist2,
                      hasTransparent  = hasTransparentColor,
                    )
                  }
                )
                wsDispatcherActors.notifyWs(ctxId.toString, wsMsg)
              }
              // Залоггировать возможную ошибку:
              for (ex <- wsNotifyFut.failed)
                LOGGER.error(s"$logPrefix Failed to send color detection result to WebSocket ctxId#${ctxId.key} cdArgs=$cdArgs cdFut=$colorDetectFut", ex)
            }
          } else {
            LOGGER.trace(s"$logPrefix Color detector already finished work. Colors histogram attached to HTTP resp.")
          }

          val result = Ok( respJson )

          // Добавить CORS-заголовки ответа?
          corsUtil.withCorsIfNeeded( result )
        }
      }

      // Функция запуска фонового удаления временных файлов:
      def __deleteUploadedFiles(): Future[_] = {
        Future {
          for (f <- request.body.data.files) {
            try {
              LOGGER.debug(s"$logPrefix Deleting tmp.uploaded file ${f.ref.path}")
              f.ref.delete()
            } catch {
              case ex: Throwable =>
                LOGGER.error(s"$logPrefix Failed to delete tmp.file ${f.ref.path}", ex)
            }
          }
        }
      }

      // Подхватить результаты работы вышестоящих for().
      resFutOpt.fold [Future[Result]] {
        __deleteUploadedFiles()
        val errMsg = errSb.toString()
        LOGGER.warn(s"$logPrefix Failed to sync-validate upload data:\n$errMsg")
        NotAcceptable( s"Problems:\n\n$errMsg" )

      } { resFut =>
        // Если файл не подхвачен файловой моделью (типа MLocalImg или другой), то его надо удалить:
        resFut.onComplete { tryRes =>
          // MLocalImg оставляем на диске, чтобы imagemagic мог с ним работать. Остальные файлы - удалять по завершению.
          if ( tryRes.fold[Boolean](
            _ => true,
            _ => !uploadArgs.info.fileHandler.isKeepUploadedFile
          ))
            __deleteUploadedFiles()
        }

        // Отрендерить ответ клиенту:
        resFut.recover { case ex: Throwable =>
          val errMsg = errSb.toString()
          LOGGER.error(s"$logPrefix Async exception occured, possible reasons:\n$errMsg", ex)
          NotAcceptable( s"Errors:\n\n$errMsg" )
        }
      }
    }
  }


  /** Поддержка возобновляемого upload'а по частям.
    * После всех chunks, надо обращаться в doFileUpload() без req body, с исходным signed qs и возможным ctxId.
    *
    * @param uploadArgs signed-метаданные, по большей части пустые, т.к. они должны быть сохранены
    *                   в disabled File-узле (disabled - чтобы не участвовал в поиске по hashesHex).
    *                   Но поле existNodeId обязательно, это id узла, в котором всё сохранено.
    * @param chunkQs Динамические метаданные для одного chunk'а.
    * @return 201 No Content - часть сохранена на отличненько.
    */
  def chunk(uploadArgs: MUploadTargetQs, chunkQs: MUploadChunkQs) = {
    // CSRF выключен, потому что session cookie не пробрасывается на ноды.
    canUpload
      .chunk( uploadArgs, chunkQs )
      .async {
        // В RAM - маленький буфер, т.к. хранить 2 метра в памяти на запрос - жирновато, а какой-то средний буфер - тут смысла не имеет.
        parse.raw(
          maxLength       = chunkQs.chunkSizeGeneral.value * 2,
          memoryThreshold = 102400,
        )
      } { implicit request =>
        lazy val logPrefix = s"chunk(${uploadArgs.info.existNodeId.orNull} ${chunkQs.chunkNumber}/${chunkQs.chunkSizeGeneral}):"

        // Сразу забрать временный файл с req.body на руки, чтобы точно удалить его по итогу работы.
        val reqBodyFile = request.body.asFile
        val reqBodyLen = request.body.size
        LOGGER.trace(s"$logPrefix Received $reqBodyLen bytes")

        import esModel.api._

        (for {
          // Посчитать и сравнить хэш-сумму части с реальной.
          hashesHexSeq2 <- fileUtil.mkHashesHexAsync(
            file    = reqBodyFile,
            hashes  = UploadConstants.CleverUp.UPLOAD_CHUNK_HASH :: Nil,
            flags   = MFileMetaHashFlags.ORIGINAL_FLAGS,
          )

          // Сверить контрольную сумму части с указанной в ссылке:
          hashesHex2 = MFileMetaHash.toHashesHex( hashesHexSeq2 )
          _ = (chunkQs.hashesHex ==* hashesHex2) || {
            val msg = s"Calculated hashes differs from qs.hashes.\n qs.hashes = ${chunkQs.hashesHex.mkString(", ")}\n calculated = ${hashesHex2.mkString(", ")}"
            LOGGER.warn(s"$logPrefix $msg")
            val r = errorHandler.onClientError( request, BAD_REQUEST, message = "" )
            throw HttpResultingException( r )
          }

          // Произвести запись в файл
          _ <- Future {
            try {
              // Запись идёт через RandomAccessFile по мотивам https://github.com/jbaysolutions/play-java-resumable/blob/master/app/controllers/HomeController.java#L34
              // Заодно, это поможет плавно задействовать LocalImgFileCreator для картинок.
              // Для системы файлов пока используем модель MLocalImg, чтобы не пере-изобретать велосипед.
              val locImgOrig = MLocalImg(
                MDynImgId(
                  origNodeId = request.mnode.id.get,
                )
              )

              if ( !mLocalImgs.isExists(locImgOrig) )
                mLocalImgs.prepareWriteFile( locImgOrig )
              val locImgOrigFile = mLocalImgs.fileOf( locImgOrig )
              LOGGER.trace(s"Move data chunk\n FROM = ${reqBodyFile}\n INTO = $locImgOrigFile")

              val raf = new RandomAccessFile( locImgOrigFile, "rw" )
              try {
                // Промотка на нужную позицию. Даже если chunkNumber0=0, всё равно мотаем в ноль - для надёжности.
                raf.seek( chunkQs.chunkNumber0 * chunkQs.chunkSizeGeneral.value )

                val reqBodyIS = new FileInputStream( reqBodyFile )
                val transferred = try {
                  // Перекачка данных из reqBodyFile в raf.
                  blocking {
                    reqBodyIS
                      .getChannel
                      .transferTo( 0, reqBodyLen, raf.getChannel )
                  }
                } finally {
                  reqBodyIS.close()
                }
                LOGGER.trace(s"$logPrefix Transferred $transferred bytes.")
              } finally {
                raf.close()
              }

            } finally {
              reqBodyFile.delete()
            }
          }

          sliceEdge = MEdge(
            predicate = MPredicates.Blob.Slice,
            media = Some( MEdgeMedia(
              file = MFileMeta(
                sizeB = Some( reqBodyLen ),
                hashesHex = hashesHexSeq2,
              ),
            )),
            doc = MEdgeDoc(
              id = Some( chunkQs.chunkNumber ),
            ),
          )

          // Сохранить в узел инфу по принятому chunk'у.
          mnodeOrNull2 <- mNodes.tryUpdate( request.mnode ) { mnode0 =>
            var modsAcc = List.empty[MNode => MNode]

            val sliceOpt0 = mnode0.edges.edgesByUid
              .get( chunkQs.chunkNumber )
              .filter( _.predicate ==* MPredicates.Blob.Slice )

            if (!(sliceOpt0 contains sliceEdge)) {
              modsAcc ::= MNode.edges
                .composeLens(MNodeEdges.out)
                .modify { out0 =>
                  var iter = out0.iterator

                  if (sliceOpt0.nonEmpty)
                    iter = iter.filterNot( _.doc.id contains[EdgeUid_t] chunkQs.chunkNumber )

                  MNodeEdges.edgesToMap1(
                    iter ++ Iterator.single( sliceEdge )
                  )
                }
            }

            // Если techName не выставлено, а имя файла есть на руках, то сохранить имя файла.
            if (mnode0.meta.basic.techName.isEmpty) {
              for ( fileName <- chunkQs.fileName if fileName.nonEmpty )
                modsAcc ::= MNode.meta
                  .composeLens( MMeta.basic )
                  .composeLens( MBasicMeta.techName )
                  .set( chunkQs.fileName )
            }

            modsAcc
              .reduceOption(_ andThen _)
              .map( _(mnode0) )
              // Теоретически возможна ситуация, когда ничего не изменилось: тогда можно вернуть null.
              .orNull
          }

          mnode2: MNode = {
            val mnode2Opt = Option( mnodeOrNull2 )

            // Сразу отправляем в кэш свежеобновлённый узел, чтобы на параллельных запросах возможно как-то снизить нагрузку.
            // Future или нет - тут не влияет, из-за особенностей реализации плагина caffeine-кэша.
            mnode2Opt.foreach( mNodes.putToCache )

            mnode2Opt getOrElse request.mnode
          }

        } yield {
          LOGGER.debug(s"$logPrefix Chunk processed ok, node#${mnode2.id.orNull} v=${mnode2.versionOpt.getOrElse(-1L)} updated with edge#${chunkQs.chunkNumber} $sliceEdge")
          Ok
        })
          .recoverHttpResEx
          .map( corsUtil.withCorsIfNeeded )
      }
  }


  /** Быстрая проверка chunk'а, вдруг он уже был закачен ранее. */
  def hasChunk(uploadArgs: MUploadTargetQs, chunkQs: MUploadChunkQs) = {
    // CSRF выключен, потому что session cookie не пробрасывается на ноды.
    canUpload
      .chunk( uploadArgs, chunkQs )
      .async( parse.raw(maxLength = chunkQs.chunkSizeGeneral.value * 2) ) { implicit request =>
        lazy val logPrefix = s"hasChunk(${uploadArgs.info.existNodeId.orNull} ${chunkQs.chunkNumber}/${chunkQs.chunkSizeGeneral}):"
        LOGGER.trace( s"$logPrefix searching for hashes: ${chunkQs.hashesHex.mkString("\n ")}" )

        val localImg = MLocalImg( MDynImgId(request.mnode.id.get) )
        val localImgFile = mLocalImgs.fileOf( localImg )
        lazy val localImgFileLen = localImgFile.length()

        val isChunkLoadedOk =
          {
            // Убедится, что файл существует и не пустой.
            val r = localImgFile.exists() && (localImgFileLen > 0)
            if (!r) LOGGER.debug(s"$logPrefix Chunk-related file not found or empty: $localImgFile len=$localImgFileLen")
            r
          } && {
            // Сравниваем просто наличие endByte
            // TODO Убедится, что в файле есть данные именно этого chunk'а? Можно пересчитать хэш, но это лишняя нагрузка...
            val endByte = chunkQs.chunkSizeGeneral.value * chunkQs.chunkNumber0 + chunkQs.chunkSizeCurrent.get
            val r = endByte <= localImgFileLen
            if (!r) LOGGER.debug(s"$logPrefix EndByte#$endByte, but FileLen=$localImgFileLen")
            r
          } &&
          chunkQs.totalSize.fold {
            LOGGER.trace(s"$logPrefix No totalSize in qs, expected=${request.fileEdgeMedia.file.sizeB.orNull}b, skipping.")
            true
          } { qsTotalSizeB =>
            val edgeSize = request.fileEdgeMedia.file.sizeB
            val r = edgeSize contains[Long] qsTotalSizeB
            if (!r) LOGGER.warn(s"$logPrefix chunk size mismatch: qs[$qsTotalSizeB] != ${edgeSize.orNull}")
            r

          } && {
            // Проверяем данные chunk'а
            val chunkEdge = request.mnode.edges
              // Ищем через UID-карту, чтобы кэшируемый узел на параллельных запросах ворочался быстрее.
              .withUid( chunkQs.chunkNumber )
              .out
            if (chunkEdge.isEmpty) {
              LOGGER.trace(s"$logPrefix Not found chunk#${chunkQs.chunkNumber}, knownChunks = [${request.mnode.edges.edgesByUid.keys.mkString(" ")}]")
              false
            } else {
              chunkEdge
                .iterator
                .filter( _.predicate ==* MPredicates.Blob.Slice )
                .exists { chunkEdge =>
                  LOGGER.trace(s"$logPrefix Found chunk#${chunkQs.chunkNumber}: $chunkEdge")
                  // Сверить chunkSizeGeneral. Сверять размер chunk'а, и прочие парамеры из qs, включая хэш-сумму.
                  // chunkSize сохраняется в edge.media.
                  chunkEdge.media.exists { chunkEdgeMedia =>
                    val cf = chunkEdgeMedia.file
                    // size-сравнивание: если оба None, то всё равно впереди сравнение хэшей.
                    val isSizeValid = (cf.sizeB ==* chunkQs.chunkSizeCurrent)
                    if (!isSizeValid) LOGGER.warn(s"$logPrefix Chunk size mismatch, qs[${chunkQs.chunkSizeGeneral.value}] != ${cf.sizeB}")
                    isSizeValid &&
                      chunkQs.hashesHex.forall { case (qsHashType, qsHashValue) =>
                        val r = cf.hashesHex.exists { storedHash =>
                          (qsHashType ==* storedHash.hType) &&
                          (qsHashValue ==* storedHash.hexValue)
                        }
                        if (!r) LOGGER.warn(s"$logPrefix Mismatch slice#${chunkQs.chunkNumber} qs.hash.${qsHashType}[$qsHashValue] != [${cf.hashesHex.mkString(" | ")}]")
                        r
                      }
                  }
                }
            }
          }

        LOGGER.trace(s"$logPrefix isOk?$isChunkLoadedOk")

        // Вернуть инфу по сверке:
        corsUtil.withCorsIfNeeded(
          if (isChunkLoadedOk) Ok
          else NoContent
        )
      }
  }


  /** Экшен, возвращающий upload-конфигурацию текущего узла.
    * Служебный, для отладки всяких проблем с балансировкой и маршрутизации.
    */
  def getConfig = ignoreAuth() { implicit request =>
    val sb = new StringBuilder(128)
    val NL = '\n'

    sb.append("my public url = ")
      .append( uploadUtil.MY_NODE_PUBLIC_HOST )
      .append(NL)

    sb.append("localhost = ")
      .append( InetAddress.getLocalHost )
      .append(NL)

    Ok( sb.toString() )
  }


  /** Экшен скачивания какого-либо файла из хранилища.
    * Дёргается в контексте *.nodes.s.io, поэтому сессия недоступна, хотя personId передаётся в qs+signed.
    *
    * @param dlQs Данные для скачки.
    * @return
    */
  def download(dlQs: MDownLoadQs) = _download(dlQs, returnBody = true)
  def downloadHead(dlQs: MDownLoadQs) = _download(dlQs, returnBody = false)
  private def _download(dlQs: MDownLoadQs, returnBody: Boolean) = {
    bruteForceProtect {
      // BruteForceProtect: тут для подавления трафика от повторяющихся запросов.
      canDownloadFile(dlQs)
        .andThen( new isFileNotModified.Refiner )
        .async( downloadLogic(
          dispInline = dlQs.dispInline,
          returnBody = returnBody,
        )(_) )
    }
  }

  def downloadLogic[A](dispInline: Boolean, returnBody: Boolean)(ctx304: Ctx304[A]): Future[Result] = {
    import ctx304.request

    lazy val logPrefix = s"downloadLogic(i?$dispInline,b?$returnBody):"

    val s = request.edgeMedia.storage.get

    val readArgs = MDsReadArgs(
      ptr    = s.data,
      params = MDsReadParams(
        returnBody        = returnBody,
        acceptCompression = request.acceptCompressEncodings,
        // Извлечь range-заголовки:
        range = for {
          rangeHdr <- ctx304.request.headers.get( RANGE )
          if rangeHdr matches "bytes=([0-9]{1,10}+-[0-9]{1,10},?\\s*)+"
        } yield {
          MDsRangeInfo(
            range = rangeHdr,
            rangeIf = ctx304.request.headers
              .get( IF_RANGE )
              .filter(_.nonEmpty),
          )
        },
      ),
    )
    val storClient = iMediaStorages.client( s.storage )
    LOGGER.trace(s"$logPrefix node#${ctx304.request.mnode.idOrNull} storClient=${storClient.getClass.getSimpleName}\n edgeMedia: ${ctx304.request.edgeMedia}\n storArgs => $readArgs")

    (for {
      ds <- storClient.read( readArgs )
    } yield {
      LOGGER.trace(s"downloadLogic()#${System.currentTimeMillis()}: Streaming download file node#${ctx304.request.mnode.idOrNull} to ${ctx304.request.remoteClientAddress}\n file = ${request.edgeMedia.file}\n storage = ${request.edgeMedia.storage.orNull}")

      val hdrs = (ACCEPT_RANGES -> "bytes") #::
        Results.contentDispositionHeader(
          inline = dispInline,
          name = request.mnode.guessDisplayName,
        )
          .to(LazyList)

      // Добавить ETag, Cache-Control, и т.д.
      isFileNotModified.with304Headers(
        ctx304,
        sendDataSource(
          dataSource = ds,
          nodeContentType = request.edgeMedia.file.mime,
        )
          .withHeaders( hdrs: _* )
      )
    })
      .recoverWith { case ex: NoSuchElementException =>
        LOGGER.debug(s"$logPrefix File #${storClient.getClass.getSimpleName}[${s.data}] not found", ex)
        errorHandler.onClientError( request, statusCode = NOT_FOUND, message = "File not found" )
      }
  }


  /** Сборка file-эджа.
    *
    * @param fileMeta Метаданные файла.
    * @param storageInfo Инфа по стораджу.
    * @param pictureMeta Данные картинки, если есть.
    * @return Эдж.
    */
  private def _mkFileEdge(fileMeta: MFileMeta, storageInfo: MStorageInfo, nodeIds: Set[String] = Set.empty,
                          pictureMeta: MPictureMeta = MPictureMeta.empty, edgeFlags: Iterable[MEdgeFlag] = Nil): MEdge = {
    MEdge(
      predicate = MPredicates.Blob.File,
      nodeIds = nodeIds,
      // nodeIds содержит publicURL целевого узла.
      media = Some( MEdgeMedia(
        file = fileMeta,
        picture = pictureMeta,
        storage = Some( storageInfo ),
      )),
      info = MEdgeInfo(
        // 2020-feb-14: Сохранять дату заливки файла в эдж, т.к. используется для Last-Modified, а другие даты узла ненадёжны.
        date   = Some( OffsetDateTime.now() ),
        flags  = edgeFlags
          .map(MEdgeFlagData.apply),
      ),
    )
  }

  /** Сборка CreateBy-эджа. */
  private def _mkCreatedByEdge( personIds: Option[String]* ): Option[MEdge] = {
    personIds
      .iterator
      .flatten
      .map { personId =>
        MEdge(
          predicate = MPredicates.CreatedBy,
          nodeIds   = Set.empty + personId,
        )
      }
      .nextOption()
  }


  /** Собрать и сохранить новый файловый узел. */
  private def _createFileNode(fileCreator: LocalImgFileCreator, nodeType: MNodeType, techName: Option[String],
                              edges: Seq[MEdge], enabled: Boolean): Future[MNode] = {
    import esModel.api._

    val mnode0 = MNode(
      id = Some( fileCreator.liArgs.mLocalImg.dynImgId.mediaId ),
      common = MNodeCommon(
        ntype         = nodeType,
        isDependent   = true,
        isEnabled     = enabled,
      ),
      meta = MMeta(
        basic = MBasicMeta(
          techName    = techName,
        ),
      ),
      edges = MNodeEdges(
        out = MNodeEdges.edgesToMap1( edges ),
      ),
    )

    for {
      mnodeId <- mNodes.save( mnode0 )
    } yield {
      LOGGER.debug(s"_createFileNode(id#$mnodeId type#$nodeType): Created, techNode=''$techName''")
      (
        (MNode.versionOpt set Some(SioEsUtil.DOC_VSN_0) ) andThen
        (MNode.id set Some(mnodeId))
      )(mnode0)
    }
  }

  private def _renderHashesHex(iter: IterableOnce[MFileMetaHash]): String = {
    iter
      .iterator
      .map(fmh => fmh.hType -> fmh.hexValue)
      .mkString(", ")
  }

}

