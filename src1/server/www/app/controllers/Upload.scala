package controllers

import java.io.File
import java.net.InetAddress
import java.nio.file.Path

import javax.inject.{Inject, Singleton}
import io.suggest.color.{MHistogram, MHistogramWs}
import io.suggest.common.empty.OptionUtil
import io.suggest.crypto.hash.MHash
import io.suggest.ctx.MCtxId
import io.suggest.es.model.{EsModel, IMust}
import io.suggest.es.util.SioEsUtil
import io.suggest.file.MSrvFileInfo
import io.suggest.file.up.{MFile4UpProps, MUploadResp}
import io.suggest.fio.WriteRequest
import io.suggest.i18n.MMessage
import io.suggest.img.MImgFmts
import io.suggest.n2.edge.{MEdge, MNodeEdges, MPredicates}
import io.suggest.n2.edge.search.{Criteria, MHashCriteria}
import io.suggest.n2.media._
import io.suggest.n2.media.storage.IMediaStorages
import io.suggest.n2.node.{MNode, MNodeType, MNodes}
import io.suggest.n2.node.common.MNodeCommon
import io.suggest.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.primo.id.IId
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.sec.av.{ClamAvScanRequest, ClamAvUtil}
import io.suggest.up.UploadConstants
import io.suggest.url.MHostUrl
import io.suggest.ws.{MWsMsg, MWsMsgTypes}
import models.im._
import models.mproj.ICommonDi
import models.mup._
import models.req.IReq
import play.api.libs.Files.{SingletonTemporaryFileCreator, TemporaryFile, TemporaryFileCreator}
import play.api.libs.json.Json
import play.api.mvc.{BodyParser, MultipartFormData, Result}
import play.core.parsers.Multipart
import util.acl.{CanUploadFile, IgnoreAuth}
import util.cdn.CdnUtil
import util.up.{FileUtil, UploadUtil}
import japgolly.univeq._
import monocle.Traversal
import util.img.ImgFileUtil
import util.img.detect.main.MainColorDetector
import util.ws.WsDispatcherActors

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scalaz.ValidationNel
import scalaz.std.option._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.10.17 12:26
  * Description: Контроллер загрузки файлов на сервера s.io.
  */
@Singleton
class Upload @Inject()(
                        esModel                   : EsModel,
                        uploadUtil                : UploadUtil,
                        canUploadFile             : CanUploadFile,
                        ignoreAuth                : IgnoreAuth,
                        cdnUtil                   : CdnUtil,
                        fileUtil                  : FileUtil,
                        iMediaStorages            : IMediaStorages,
                        mNodes                    : MNodes,
                        mImgs3                    : MImgs3,
                        mLocalImgs                : MLocalImgs,
                        clamAvUtil                : ClamAvUtil,
                        imgFileUtil               : ImgFileUtil,
                        uploadCtxFactory          : IUploadCtxFactory,
                        mainColorDetector         : MainColorDetector,
                        wsDispatcherActors        : WsDispatcherActors,
                        sioControllerApi          : SioControllerApi,
                        mCommonDi                 : ICommonDi,
                      )
  extends MacroLogsImpl
{

  import sioControllerApi._
  import mCommonDi.{ec, errorHandler}
  import esModel.api._


  // TODO Opt В будущем, особенно когда будет поддержка заливки видео (или иных больших файлов), надо будет
  // переписать body parser, чтобы возвращал просто Source[ByteString, _].
  // В качестве параллельных sink'ов надо сразу: swfs, clamav, hashesHex, colorDetector?, ffmpeg, etc... При ошибке - удалять из swfs.

  /** Body-parser для prepareUploadLogic. */
  def prepareUploadBp: BodyParser[MFile4UpProps] = {
    parse.json[MFile4UpProps]
  }

  /** Тело экшена подготовки к аплоаду.
    * Только тело, потому что ACL-проверки выносятся в основной контроллер, в контексте которого происходит загрузка.
    *
    * @param nodeType Тип (создаваемого) узла.
    *                 Узел файла указанного типа будет создан при успешной заливке файла.
    * @param validated Провалидированные JSON-метаданные файла.
    * @param uploadFileHandler Если требуется принимать файл не в /tmp/, а сразу куда-то, то здесь Some().
    *
    * @return Created | Accepted | NotAcceptable  + JSON-body в формате MFile4UpProps.
    */
  def prepareUploadLogic(logPrefix          : String,
                         nodeType           : MNodeType,
                         validated          : ValidationNel[String, MFile4UpProps],
                         uploadFileHandler  : Option[MUploadFileHandler] = None,
                         colorDetect        : Option[MColorDetectArgs] = None,
                        )(implicit request: IReq[MFile4UpProps]) : Future[Result] = {
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
      {upFileProps =>
        LOGGER.trace(s"$logPrefix Body validated, user#${request.user.personIdOpt.orNull}:\n ${request.body} => $upFileProps")

        for {
          // Поискать файл с такими параметрами в MMedia:
          fileSearchRes <- mNodes.dynSearch(
            new MNodeSearch {
              // Тут по предикату - надо ли фильтровать?
              val crs = Criteria(
                fileSizeB = upFileProps.sizeB :: Nil,
                fileHashesHex = (for {
                  (mhash, hexValue) <- upFileProps.hashesHex.iterator
                } yield {
                  MHashCriteria(
                    hTypes    = mhash :: Nil,
                    hexValues = hexValue :: Nil,
                    must      = IMust.MUST
                  )
                })
                  .toSeq,
              ) :: Nil
              override def outEdges = crs
              override def limit = 1
            }
          )

          (respStatus, respDataFut) = fileSearchRes
            .headOption
            .fold [(Status, Future[MUploadResp])] {
              val assignRespFut = cdnUtil.assignDist(upFileProps)
              LOGGER.trace(s"$logPrefix No existing file, user will upload a new file.")

              val upDataFut = for {
                assignResp <- assignRespFut
              } yield {
                LOGGER.trace(s"$logPrefix Assigned swfs resp: $assignResp")

                val upData = MUploadTargetQs(
                  fileProps   = upFileProps,
                  fileHandler = uploadFileHandler,
                  personId    = request.user.personIdOpt,
                  validTillS  = uploadUtil.ttlFromNow(),
                  storage     = assignResp,
                  colorDetect = colorDetect,
                  nodeType    = nodeType,
                )
                // Список хостнеймов: в будущем возможно, что ссылок для заливки будет несколько: основная и запасная. Или ещё что-то.
                val hostnames = List(
                  assignResp.host.namePublic
                  // TODO Вписать запасные хостнеймы для аплоада?
                )
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

            } { foundFileNode =>
              val fileEdge = foundFileNode
                .edges
                .withPredicateIter( MPredicates.File )
                .next()
              val edgeMedia = fileEdge.media.get

              val existColors = edgeMedia
                .picture
                .histogram
                .colorsOrNil

              LOGGER.debug(s"$logPrefix Found existing file media#${foundFileNode.idOrNull} for hashes [${upFileProps.hashesHex.valuesIterator.mkString(", ")}] with ${existColors.size} colors.")

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
                    e <- origMedia.edges.withPredicateIter( MPredicates.File )
                    edgeMedia <- e.media
                    histogram <- edgeMedia.picture.histogram
                    if histogram.colors.nonEmpty
                  } yield {
                    histogram
                  })
                    .buffered
                    .headOption
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
                    url       = if (edgeMedia.file.imgFormatOpt.nonEmpty) {
                      // TODO IMG_DIST: Вписать хост расположения картинки.
                      // TODO Нужна ссылка картинки на недо-оригинал картинки? Или как?
                      Some( routes.Img.dynImg( foundFileImg ).url )
                    } else {
                      // TODO IMG_DIST Надо просто универсальную ссылку для скачивания файла, независимо от его типа.
                      LOGGER.error(s"$logPrefix MIME ${edgeMedia.file.mime} don't know how to build URL")
                      None
                    },
                    // TODO foundFile.file.dateCreated - не раскрывать. Лучше удалить MFileMeta.dateCreated после переезда в MEdge.media
                    fileMeta  = Some( edgeMedia.file ),
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
                  )),
                )
              }
              (Accepted, upRespFut)
            }
          respData <- respDataFut
        } yield {
          respStatus( Json.toJson(respData) )
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
          val imgFmt = MImgFmts.withMime(uploadArgs.fileProps.mimeType).get
          val dynImgId = MDynImgId.randomOrig( imgFmt )
          LocalImgFileCreator( MLocalImg(dynImgId) )
      }
    // Сборка самого BodyParser'а.
    val bp0 = parse.multipartFormData(
      Multipart.handleFilePartAsTemporaryFile( fileHandler ),
      maxLength = uploadArgs.fileProps.sizeB + 10000L
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
    *
    * @param uploadArgs Подписанные данные аплоада
    * @param ctxIdOpt Подписанный (но пока непроверенный) ctxId.
    * @return Ok + MUploadResp | NotAcceptable
    */
  def doFileUpload(uploadArgs: MUploadTargetQs, ctxIdOpt: Option[MCtxId]) = {
    // Csrf выключен, потому что session cookie не пробрасывается на ноды.
    val bp = _uploadFileBp(uploadArgs)

    canUploadFile(uploadArgs, ctxIdOpt).async(bp) { implicit request =>
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
        filePart <- {
          val partName = UploadConstants.MPART_FILE_FN
          val fpOpt = request.body.data.file( partName )
          LOGGER.trace(s"$logPrefix File part '$partName' lookup: found?${fpOpt.nonEmpty}: ${fpOpt.orNull}")
          if (fpOpt.isEmpty)
            __appendErr(s"Missing file part with name '$partName'.")
          fpOpt
        }

        // Проверить Content-Type, заявленный в теле запроса:
        if {
          val r = filePart.contentType
            .exists(_ equalsIgnoreCase uploadArgs.fileProps.mimeType)
          LOGGER.trace(s"$logPrefix Content-type verify: expected=${uploadArgs.fileProps.mimeType} filePart=${filePart.contentType} ;; match?$r")
          if (!r)
            __appendErr( s"Expected Content-type '${uploadArgs.fileProps.mimeType}' not matching to Multipart part's content-type '${filePart.contentType.orNull}'." )
          r
        }

        // Сборка Upload-контекста. Дальнейший сбор информации по загруженному файлу должен происходить в контексте.
        upCtx = uploadCtxFactory.make(filePart, uploadArgs, request.body.localImg)

        // Сверить размер файла с заявленным размером
        if {
          val srcLen = upCtx.fileLength
          val r = srcLen ==* uploadArgs.fileProps.sizeB
          LOGGER.trace(s"$logPrefix File size check: expected=${uploadArgs.fileProps.sizeB} detected=$srcLen ;; match? $r")
          if (!r)
            __appendErr(s"Size of file $srcLen bytes, but expected is ${uploadArgs.fileProps.sizeB} bytes.")
          r
        }

        // Сверить MIME-тип принятого файла с заявленным:
        detectedMimeType <- upCtx.detectedMimeTypeOpt
        if {
          val r = detectedMimeType ==* upCtx.declaredMime
          if (!r)
            __appendErr( s"Detected file MIME type '${upCtx.detectedMimeTypeOpt}' does not match to expected ${upCtx.declaredMime}." )
          r
        }

        // Прежде чем запускать тяжелые проверки, надо быстро сверить лимиты для текущего типа файла:
        if {
          val r = upCtx.validateFileContentEarly
          if (!r)
            __appendErr( s"Failed to validate size limits: len=${upCtx.fileLength}b img=${upCtx.imgFmtOpt.orNull}/${upCtx.imageWh.orNull}" )
          r
        }

      } yield {
        val startMs = System.currentTimeMillis()
        // Лёгкие синхронные проверки завершены успешно. Переходим в асинхрон и в тяжелые проверки.

        // В фоне запустить JVM-only валидацию содержимого файла. Все файлы должны иметь корректный внутренний формат.
        val isFileValidFut = upCtx.validateFileFut

        for {
          // Рассчитать хэш-суммы файла (digest).
          hashesHex2 <- upCtx.hashesHexFut
          hashesHexMap = IId.els2idMap[MHash, MFileMetaHash]( hashesHex2 )

          // Сверить рассчётные хэш-суммы с заявленными при загрузке.
          if {
            uploadArgs.fileProps
              .hashesHex
              .forall { case (mhash, expectedHexVal) =>
                hashesHexMap
                  .get(mhash)
                  .exists { mfhash =>
                    val r = mfhash.hexValue ==* expectedHexVal
                    if (!r) {
                      LOGGER.error(s"$logPrefix $mfhash != $mhash $expectedHexVal file=${upCtx.file} ${upCtx.fileLength}b user=${request.user.personIdOpt.orNull}")
                      errSb.synchronized {
                        __appendErr( s"File hash ${mhash.fullStdName} '${mfhash.hexValue}' doesn't match to declared '$expectedHexVal'." )
                      }
                    }
                    r
                  }
              }
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

          fileNameOpt = Option( filePart.filename )

          // Собираем MediaStorage ptr и клиент:
          storageInfo = uploadArgs.storage.storage
          storageClient = iMediaStorages.client( storageInfo.storage )

          // TODO Если требуется img-форматом, причесать оригинал, расширив исходную карту хэшей новыми значениями.
          // Например, JPEG можно пропустить через jpegtran -copy.

          // Запускаем в фоне заливку файла из ФС в надёжное распределённое хранилище:
          saveFileToShardFut = {
            val wr = WriteRequest(
              contentType  = detectedMimeType,
              file         = upCtx.file,
              origFileName = fileNameOpt
            )
            LOGGER.trace(s"$logPrefix Will save file $wr to storage $storageInfo ...")
            storageClient.write( storageInfo.data, wr )
          }

          // Проверки закончены. Пора переходить к действиям по сохранению узла, сохранению и анализу файла.
          mnode0 = MNode(
            id = request.body.localImg
              .map( _.dynImgId.mediaId ),
            common = MNodeCommon(
              ntype         = uploadArgs.nodeType,
              isDependent   = true,
            ),
            meta = MMeta(
              basic = MBasicMeta(
                techName    = Option( filePart.filename ),
              ),
            ),
            edges = MNodeEdges(
              out = {
                val fileEdge = MEdge(
                  predicate = MPredicates.File,
                  media = Some( MEdgeMedia(
                    file = MFileMeta(
                      mime       = detectedMimeType,
                      sizeB      = uploadArgs.fileProps.sizeB,
                      isOriginal = true,
                      hashesHex  = hashesHex2,
                    ),
                    picture = MPictureMeta(
                      whPx = upCtx.imageWh,
                    ),
                    storage = storageInfo,
                  )),
                )

                // Записываем id юзера, который первым загрузил этот файл.
                val createdBy = request.user
                  .personIdOpt
                  .fold( List.empty[MEdge] ) { personId =>
                    MEdge(
                      predicate = MPredicates.CreatedBy,
                      nodeIds   = Set( personId ),
                    ) :: Nil
                  }

                MNodeEdges.edgesToMap1( fileEdge :: createdBy )
              },
            ),
          )

          // Создаём новый узел для загруженного файла.
          mnodeIdFut = {
            val fut = mNodes.save( mnode0 )

            // При ошибке сохранения узла надо удалить файл из saveFileToShardFut
            for {
              _ <- fut.failed
              _ <- {
                LOGGER.error(s"$logPrefix Failed to save MNode#${mnode0.id.orNull}. Deleting file storage#$storageInfo...")
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
            mimg        <- request.body.localImg
            cdArgs      <- uploadArgs.colorDetect
          } yield {
            mainColorDetector.cached(mimg) {
              mainColorDetector.detectPaletteFor(mimg, maxColors = cdArgs.paletteSize)
            }
          }

          // Ожидаем окончания сохранения узла.
          mnodeId <- mnodeIdFut

          // Собрать в голове сохранённый инстанс MMedia:  // TODO надо бы lazy, т.к. он может не понадобится.
          mnode1 = {
            LOGGER.debug(s"$logPrefix Created MNode#$mnodeId")
            (
              (MNode.versionOpt set Some(SioEsUtil.DOC_VSN_0) ) andThen
              (MNode.id set Some(mnodeId))
            )(mnode0)
          }

          // Потом в фоне вне основного экшена сохранить результат детектирования основных цветов картинки в MMedia.PictureMeta:
          _ = {
            mNodes.putToCache( mnode1 )

            for (colorDetectFut <- colorDetectFutOpt) {
              val saveColorsFut = for (colorHist <- colorDetectFut) yield {
                if (colorHist.colors.nonEmpty) {
                  // Считаем общее кол-во пикселей для нормировки частот цветов:
                  val colorsHist2 = colorHist.withRelFrequences
                  lazy val colorsCount = colorsHist2.colors.size

                  LOGGER.trace(s"$logPrefix Detected $colorsCount top-colors on node#$mnodeId:\n ${colorsHist2.colors.iterator.map(_.hexCode).mkString(", ")}")

                  val mmedia2OptFut = mNodes.tryUpdate(mnode1)(
                    MNode.edges.modify { edges0 =>
                      MNodeEdges.out.set(
                        MNodeEdges.edgesToMap1(
                          edges0
                            .withPredicateIter( MPredicates.File )
                            .map(
                              MEdge.media
                                .composeTraversal( Traversal.fromTraverse[Option, MEdgeMedia] )
                                .composeLens( MEdgeMedia.picture )
                                .composeLens( MPictureMeta.histogram )
                                .set( Some(colorsHist2) )
                            )
                        )
                      )(edges0)
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
                LOGGER.error(s"$logPrefix Failed to send file into storage#$storageInfo . Deleting MNode#$mnodeId ...", ex)
                mNodes.deleteById( mnodeId )
              }

              // Запустить в фоне чистку хранилища, на случай если там что-то всё-таки осело, несмотря на ошибку.
              _ = storageClient
                .delete( storageInfo.data )
                .onComplete { tryRes =>
                  LOGGER.debug(s"$logPrefix Storage clean-up after error => $tryRes")
                }

              delNodeRes <- delNodeResFut
            } {
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
          val srvFileInfo = MSrvFileInfo(
            nodeId    = mnodeId,
            // TODO url: Надо бы вернуть ссылку. Для img - routes.Img.*, для других - по другим адресам.
            // Нет необходимости слать это всё назад, поэтому во всех заведомо известных клиенту поля None или empty:
            pictureMeta = MPictureMeta(
              histogram = colorsOpt,
            )
          )
          val resp = MUploadResp(
            fileExist = Some(srvFileInfo)
          )
          val respJson = Json.toJson(resp)

          // side-effect: Если оказалось, что MainColorDetector не успел всё сделать к текущему моменту (это нормально),
          // то запустить передачу данных от него по websocket'у:
          // Отправляем как можно ПОЗЖЕ, чтобы максимально снизить race conditions между веб-сокетом и результатом экшена.
          if (colorsOpt.isEmpty) {
            LOGGER.trace(s"$logPrefix ColorDetector not completed yet. Connect it with websocket.")
            for {
              colorDetectFut <- colorDetectFutOpt
              cdArgs         <- uploadArgs.colorDetect
              ctxId          <- ctxIdOpt
              hasTransparentColorFut <- upCtx.imageHasTransparentColors
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
                      hasTransparent  = hasTransparentColor
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

          // HTTP-ответ
          Ok( respJson )
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
        errorHandler.onClientError(request, NOT_ACCEPTABLE, s"Problems:\n\n$errMsg")

      } { resFut =>
        // Если файл не подхвачен файловой моделью (типа MLocalImg или другой), то его надо удалить:
        resFut.onComplete { tryRes =>
          if ((tryRes.isSuccess && request.body.isDeleteFileOnSuccess) || tryRes.isFailure)
            __deleteUploadedFiles()
        }

        // Отрендерить ответ клиенту:
        resFut.recoverWith { case ex: Throwable =>
          val errMsg = errSb.toString()
          LOGGER.error(s"$logPrefix Async exception occured, possible reasons:\n$errMsg", ex)
          errorHandler.onClientError(request, NOT_ACCEPTABLE, s"Errors:\n\n$errMsg")
        }
      }
    }
  }


  /** Реализация перехвата временных файлов сразу в MLocalImg-хранилище. */
  protected case class LocalImgFileCreator(mLocalImg: MLocalImg)
    extends TemporaryFileCreator { creator =>

    override def create(prefix: String, suffix: String): TemporaryFile = _create()

    override def create(path: Path): TemporaryFile = _create()

    private def _create(): TemporaryFile = {
      mLocalImgs.prepareWriteFile( mLocalImg )
      LocalImgFile
    }

    override def delete(file: TemporaryFile): Try[Boolean] = {
      Try {
        mLocalImgs.deleteAllSyncFor(mLocalImg.dynImgId.origNodeId)
        true
      }
    }

    /** Маскировка MLocalImg под TemporaryFile. */
    case object LocalImgFile extends TemporaryFile {

      private val _file = mLocalImgs.fileOf( mLocalImg )

      override def path: Path = _file.toPath
      override def file: File = _file
      override def temporaryFileCreator = creator
    }

  }



  /** Экшен, возвращающий upload-конфигурацию текущего узла.
    * Служебный, для отладки всяких проблем с балансировкой и маршрутизации.
    */
  def getConfig = ignoreAuth() { implicit request =>
    val sb = new StringBuilder(128)
    val NL = '\n'

    sb.append("my public url = ")
      .append( uploadUtil.MY_NODE_PUBLIC_URL )
      .append(NL)

    sb.append("localhost = ")
      .append( InetAddress.getLocalHost )
      .append(NL)

    Ok( sb.toString() )
  }

}

/** Контейнер результата работы BodyParser'а при аплоаде.
  * Вынесен за пределы контроллера из-за проблем с компиляцией routes, если это inner-class.
  */
protected class UploadBpRes(
                             val data     : MultipartFormData[TemporaryFile],
                             val localImg : Option[MLocalImg]
                           ) {

  /** Надо ли удалять залитый файл? */
  def isDeleteFileOnSuccess: Boolean = {
    // Да, если файл не подхвачен какой-либо файловой моделью (MLocalImg, например).
    localImg.isEmpty
  }

}

