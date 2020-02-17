package controllers

import java.io.File
import java.net.InetAddress
import java.nio.file.Path
import java.time.OffsetDateTime

import javax.inject.Inject
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
import io.suggest.n2.edge.edit.{MEdgeWithId, MNodeEdgeIdQs}
import io.suggest.n2.edge.{MEdge, MEdgeInfo, MNodeEdges, MPredicates}
import io.suggest.n2.edge.search.{Criteria, MHashCriteria}
import io.suggest.n2.media._
import io.suggest.n2.media.storage.IMediaStorages
import io.suggest.n2.node.{MNode, MNodes}
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
import models.mup._
import models.req.IReq
import play.api.libs.Files.{SingletonTemporaryFileCreator, TemporaryFile, TemporaryFileCreator}
import play.api.libs.json.Json
import play.api.mvc.{BodyParser, Result, Results}
import play.core.parsers.Multipart
import util.acl.{BruteForceProtect, CanDownloadFile, CanUploadFile, IgnoreAuth, IsFileNotModified}
import util.cdn.{CdnUtil, CorsUtil}
import util.up.{FileUtil, UploadUtil}
import japgolly.univeq._
import monocle.Traversal
import util.img.detect.main.MainColorDetector
import util.ws.WsDispatcherActors
import io.suggest.ueq.UnivEqUtil._
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
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

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val uploadUtil = injector.instanceOf[UploadUtil]
  private lazy val canUploadFile = injector.instanceOf[CanUploadFile]
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
  implicit private lazy val ec: ExecutionContext = injector.instanceOf[ExecutionContext]

  import sioControllerApi._


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
    * @param validated Провалидированные JSON-метаданные файла.
    * @param upInfo Контейнер данных, пробрасываемых целиком во вторую фазу.
    *
    * @return Created | Accepted | NotAcceptable  + JSON-body в формате MFile4UpProps.
    */
  def prepareUploadLogic(logPrefix          : String,
                         validated          : ValidationNel[String, MFile4UpProps],
                         upInfo             : MUploadInfoQs,
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
        import esModel.api._
        LOGGER.trace(s"$logPrefix Body validated, user#${request.user.personIdOpt.orNull}:\n ${request.body} => $upFileProps")

        for {
          // Поискать файл с такими параметрами в MMedia:
          fileSearchRes <- mNodes.dynSearch(
            new MNodeSearch {
              // Тут по предикату - надо ли фильтровать?
              override val outEdges = {
                val cr = Criteria(
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
                )
                cr :: Nil
              }
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
                  personId    = request.user.personIdOpt,
                  validTillS  = uploadUtil.ttlFromNow(),
                  storage     = assignResp,
                  info        = upInfo,
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
                    storage = Option.when( upInfo.systemResp contains true )(edgeMedia.storage),
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
  private def _uploadFileBp(uploadArgs: MUploadTargetQs): BodyParser[MUploadBpRes] = {
    // Если картинка, то заливать сразу в MLocalImg/ORIG.
    val localImgFileHandler = uploadArgs.info.fileHandler.map {
      // Команда к сохранению напрямую в MLocalImg: сообрать соответствующий FileHandler.
      case MUploadFileHandlers.Image =>
        val imgFmt = MImgFmts
          .withMime( uploadArgs.fileProps.mimeType )
          .get
        val dynImgId = MDynImgId.randomOrig( imgFmt )
        val localImg = MLocalImg( dynImgId )
        val args = MLocalImgFileCreatorArgs( localImg, imgFmt )
        LocalImgFileCreator( args )
    }

    val fileHandler: TemporaryFileCreator = localImgFileHandler
      .getOrElse( SingletonTemporaryFileCreator )

    // Сборка самого BodyParser'а.
    val bp0 = parse.multipartFormData(
      Multipart.handleFilePartAsTemporaryFile( fileHandler ),
      maxLength = uploadArgs.fileProps.sizeB + 10000L,
    )

    // Завернуть итог парсинга в контейнер, содержащий данные MLocalImg или иные возможные поля.
    for (mpfd <- bp0) yield {
      MUploadBpRes(
        data = mpfd,
        localImgArgs = fileHandler match {
          case lifc: LocalImgFileCreator => Some( lifc.liArgs )
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
        upCtxArgs = MUploadCtxArgs(filePart, uploadArgs, request.body)
        upCtx = uploadUtil.makeUploadCtx( upCtxArgs )

        // Сверить размер файла с заявленным размером
        if {
          val srcLen = upCtxArgs.fileLength
          val r = srcLen ==* uploadArgs.fileProps.sizeB
          LOGGER.trace(s"$logPrefix File size check: expected=${uploadArgs.fileProps.sizeB} detected=$srcLen ;; match? $r")
          if (!r)
            __appendErr(s"Size of file $srcLen bytes, but expected is ${uploadArgs.fileProps.sizeB} bytes.")
          r
        }

        // Определить MIME-тип принятого файла:
        detectedMimeType <- upCtxArgs.detectedMimeTypeOpt
        // Бывает, что MIME не совпадает. Решаем, что нужно делать согласно настройкам аплоада. Пример:
        // Detected file MIME type [application/zip] does not match to expected [application/vnd.android.package-archive]
        mimeType = {
          if (detectedMimeType ==* upCtxArgs.declaredMime) {
            detectedMimeType
          } else {
            val msg = s"Detected file MIME type '${upCtxArgs.detectedMimeTypeOpt}' does not match to expected ${upCtxArgs.declaredMime}."
            LOGGER.warn(s"$logPrefix $msg")
            uploadArgs.info.obeyMime.fold {
              __appendErr( msg )
              throw new NoSuchElementException(msg)
            } {
              case false =>
                LOGGER.info(s"$logPrefix Client-side MIME [${upCtxArgs.declaredMime}] replaced with server-side detected MIME: [$detectedMimeType]")
                detectedMimeType
              case true =>
                LOGGER.info(s"$logPrefix Client-side MIME [${upCtxArgs.declaredMime}] will be saved, instead of detected MIME [$detectedMimeType]")
                upCtxArgs.declaredMime
            }
          }
        }

        // Прежде чем запускать тяжелые проверки, надо быстро сверить лимиты для текущего типа файла:
        if {
          val r = upCtx.validateFileContentEarly
          if (!r)
            __appendErr( s"Failed to validate size limits: len=${upCtxArgs.fileLength}b img=${request.body.imgFmt.orNull}/${upCtx.imageWh.orNull}" )
          r
        }

      } yield {
        import esModel.api._
        val startMs = System.currentTimeMillis()
        // Лёгкие синхронные проверки завершены успешно. Переходим в асинхрон и в тяжелые проверки.

        // В фоне запустить JVM-only валидацию содержимого файла. Все файлы должны иметь корректный внутренний формат.
        val isFileValidFut = upCtx.validateFileFut

        for {
          // Рассчитать хэш-суммы файла (digest).
          hashesHex2 <- fileUtil.mkHashesHexAsync(
            file    = upCtxArgs.file,
            hashes  = uploadArgs.fileProps.hashesHex.keys,
            flags   = Set( MFileMetaHashFlags.TrulyOriginal ),
          )
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
                      LOGGER.error(s"$logPrefix $mfhash != $mhash $expectedHexVal file=${upCtxArgs.file} ${upCtxArgs.fileLength}b user=${request.user.personIdOpt.orNull}")
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
              file = upCtxArgs.file.getAbsolutePath
            )
          )

          clamAvScanRes <- clamAvScanResFut
          if {
            val r = clamAvScanRes.isClean
            LOGGER.trace(s"ClamAV: clean?$r took=${System.currentTimeMillis() - startMs} ms. ret=$clamAvScanRes")
            if (!r) {
              LOGGER.warn(s"ClamAV INFECTED $clamAvScanRes for file ${upCtxArgs.path}. See logs upper.\n User session: ${request.user.personIdOpt.orNull}\n remote: ${request.remoteClientAddress}\n User-Agent: ${request.headers.get(USER_AGENT).orNull}")
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
              contentType  = mimeType,
              file         = upCtxArgs.file,
              origFileName = fileNameOpt
            )
            LOGGER.trace(s"$logPrefix Will save file $wr to storage $storageInfo ...")
            storageClient.write( storageInfo.data, wr )
          }

          fileMeta = MFileMeta(
            mime       = Some( mimeType ),
            sizeB      = Some( uploadArgs.fileProps.sizeB ),
            isOriginal = true,
            hashesHex  = hashesHex2,
          )

          fileEdge = MEdge(
            predicate = MPredicates.File,
            media = Some( MEdgeMedia(
              file = fileMeta,
              picture = MPictureMeta(
                whPx = upCtx.imageWh,
              ),
              storage = storageInfo,
            )),
            info = MEdgeInfo(
              // 2020-feb-14: Сохранять дату заливки файла в эдж, т.к. используется для Last-Modified, а другие даты узла ненадёжны.
              dateNi = Some( OffsetDateTime.now() ),
            )
          )

          // Проверки закончены. Пора переходить к действиям по сохранению узла, сохранению и анализу файла.
          mnode1Fut = {
            // Записываем id юзера, который первым загрузил этот файл.
            val createdBy = request.user
              .personIdOpt
              .fold( List.empty[MEdge] ) { personId =>
                MEdge(
                  predicate = MPredicates.CreatedBy,
                  nodeIds   = Set( personId ),
                ) :: Nil
              }

            val addEdges = fileEdge :: createdBy

            // Собрать/обновить и сохранить узел в БД.
            val fut = request.existNodeOpt.fold {
              // Создание нового узла под файл.
              val mnode0 = MNode(
                id = request.body.localImgArgs
                  .map( _.mLocalImg.dynImgId.mediaId ),
                common = MNodeCommon(
                  ntype         = uploadArgs.info.nodeType.get,
                  isDependent   = true,
                ),
                meta = MMeta(
                  basic = MBasicMeta(
                    techName    = Option( filePart.filename ),
                  ),
                ),
                edges = MNodeEdges(
                  out = MNodeEdges.edgesToMap1( addEdges ),
                ),
              )
              for {
                mnodeId <- mNodes.save( mnode0 )
              } yield {
                LOGGER.debug(s"$logPrefix Created MNode#$mnodeId")
                (
                  (MNode.versionOpt set Some(SioEsUtil.DOC_VSN_0) ) andThen
                  (MNode.id set Some(mnodeId))
                )(mnode0)
              }

            } { mnode0 =>
              // Полулегальный патчинг эджей существующего файлового узла новым файлом.
              // Удаление старого файла будет после сохранения узла.
              LOGGER.trace(s"$logPrefix Will update existing node#${mnode0.idOrNull} with edges: ${addEdges.mkString(", ")}")

              mNodes.tryUpdate( mnode0 )(
                MNode.node_meta_basic_dateEdited_RESET andThen
                MNode.edges.modify { edges0 =>
                  val edgesSeq2 = MNodeEdges.edgesToMap1(
                    edges0.withoutPredicateIter( MPredicates.CreatedBy, MPredicates.File ) ++ addEdges
                  )
                  MNodeEdges.out.set( edgesSeq2 )(edges0)
                }
              )
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
            localImgArgs    <- request.body.localImgArgs
            cdArgs          <- uploadArgs.info.colorDetect
          } yield {
            mainColorDetector.cached( localImgArgs.mLocalImg ) {
              mainColorDetector.detectPaletteFor( localImgArgs.mLocalImg, maxColors = cdArgs.paletteSize)
            }
          }

          // Ожидаем окончания сохранения узла.
          mnode1 <- mnode1Fut

          // Файл сохранён. Но если был патчинг уже существующего узла, то надо убрать старый файл из хранилища.
          _ = {
            // Поискать старый файловый эдж для удаления.
            request
              .existNodeOpt
              .fold [Future[_]] ( Future.successful(()) ) { mnode0 =>
                val fut = for {
                  files4deleteOpts <- Future.traverse(
                    // В старом узле, в норме, всегда 0 или 1 файловый эдж, поэтому оптимизировать ничего не требуется:
                    mnode0.edges
                      .withPredicateIter( MPredicates.File )
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
                      val pred = MPredicates.File
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
          val isSystemResp = uploadArgs.info.systemResp contains true
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
          corsUtil.isSioOrigin().fold {
            // В dev-режиме - отсутствие CORS - это норма. т.к. same-origin и для страницы, и для этого экшена.
            LOGGER.debug( s"$logPrefix Not adding CORS headers, missing/invalid Origin: ${request.headers.get(ORIGIN).orNull}" )
            result
          } { _ =>
            // На продакшене - аплоад идёт на sX.nodes.suggest.io, а реквест из suggest.io - поэтому CORS тут участвует всегда.
            corsUtil.withCorsHeaders( result )
          }
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
          if ((tryRes.isSuccess && request.body.isDeleteFileOnSuccess) || tryRes.isFailure)
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


  /** Реализация перехвата временных файлов сразу в MLocalImg-хранилище. */
  protected case class LocalImgFileCreator( liArgs: MLocalImgFileCreatorArgs )
    extends TemporaryFileCreator { creator =>

    override def create(prefix: String, suffix: String): TemporaryFile = _create()

    override def create(path: Path): TemporaryFile = _create()

    private def _create(): TemporaryFile = {
      mLocalImgs.prepareWriteFile( liArgs.mLocalImg )
      LocalImgFile
    }

    override def delete(file: TemporaryFile): Try[Boolean] = {
      Try {
        mLocalImgs.deleteAllSyncFor( liArgs.mLocalImg.dynImgId.origNodeId )
        true
      }
    }

    /** Маскировка MLocalImg под TemporaryFile. */
    case object LocalImgFile extends TemporaryFile {

      private val _file = mLocalImgs.fileOf( liArgs.mLocalImg )

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


  /** Экшен скачивания какого-либо файла из хранилища.
    * Дёргается в контексте *.nodes.s.io, поэтому сессия недоступна, хотя personId передаётся в qs+signed.
    *
    * @param dlQs Данные для скачки.
    * @return
    */
  def download(dlQs: MDownLoadQs) = bruteForceProtect {
    // BruteForceProtect: тут для подавления трафика от повторяющихся запросов.
    canDownloadFile(dlQs)
      .andThen( isFileNotModified.Refiner )
      .async { ctx304 =>
        import ctx304.request

        val s = request.edgeMedia.storage
        for {
          ds <- iMediaStorages
            .client( s.storage )
            .read( s.data, request.acceptCompressEncodings )
        } yield {
          LOGGER.trace(s"download($dlQs): Streaming download file node#${dlQs.nodeId} to ${dlQs.clientAddr}\n file = ${request.edgeMedia.file}\n storage = ${request.edgeMedia.storage}")
          ctx304.with304Headers(
            sendDataSource(ds, contentType = request.edgeMedia.file.mime)
              .withHeaders(
                Results.contentDispositionHeader(
                  inline = dlQs.dispInline,
                  name = request.mnode.guessDisplayName,
                )
                  .toSeq: _*
              )
          )
        }
      }
  }

}

