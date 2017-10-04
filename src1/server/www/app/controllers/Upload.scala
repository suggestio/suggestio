package controllers

import javax.inject.{Inject, Singleton}

import io.suggest.es.model.IMust
import io.suggest.file.MSrvFileInfo
import io.suggest.file.up.{MFile4UpProps, MUploadResp}
import io.suggest.i18n.MMessage
import io.suggest.model.n2.media.{MFileMetaHash, MMedias}
import io.suggest.model.n2.media.search.{MHashCriteria, MMediaSearchDfltImpl}
import io.suggest.model.n2.media.storage.MStorages
import io.suggest.pick.MimeConst
import io.suggest.swfs.client.ISwfsClient
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.scalaz.ScalazUtil.Implicits._
import models.im.MImg3
import models.mproj.ICommonDi
import models.mup.MUploadTargetQs
import models.req.IReq
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.libs.json.Json
import play.api.mvc.Result
import play.core.parsers.Multipart
import util.acl.CanUploadFile
import util.up.UploadUtil
import views.html.helper.CSRF

import scala.concurrent.Future
import scalaz.ValidationNel

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.10.17 12:26
  * Description: Контроллер загрузки файлов на сервера s.io.
  */
@Singleton
class Upload @Inject()(
                        swfsClient                : ISwfsClient,
                        mMedias                   : MMedias,
                        uploadUtil                : UploadUtil,
                        canUploadFile             : CanUploadFile,
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
    */
  def prepareUploadLogic(logPrefix: String, validated: ValidationNel[String, MFile4UpProps])
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
              val upDataFut = for {
                assignResp <- swfsClient.assign()
              } yield {
                LOGGER.trace(s"$logPrefix No existing file, user will upload a new file.")
                val upData = MUploadTargetQs(
                  hashesHex   = upFileProps.hashesHex,
                  mimeType    = upFileProps.mimeType,
                  fileSizeB   = upFileProps.sizeB,
                  personId    = request.user.personIdOpt,
                  validTillS  = uploadUtil.ttlFromNow(),
                  storage     = Some( MStorages.SeaWeedFs ),
                  // TODO Зарезервировать fid в swfs, по fid определить ноду для фактической заливки файла, дописать хост в ссылку.
                  storInfo    = Some( assignResp.fid )
                )
                MUploadResp(
                  // TODO IMG_DIST Надо absURL, включающее в себя хост, в который производить заливку файла.
                  upUrl = Some(
                    CSRF( routes.Upload.doFileUpload(upData) ).url
                  )
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

  /** BodyParser для приёма файлов. */
  private def _uploadFileBp(maxLength: Long) = parse.multipartFormData(
    Multipart.handleFilePartAsTemporaryFile( SingletonTemporaryFileCreator ),
    maxLength = maxLength
  )


  /** Экшен фактического аплоада файла.
    *
    * @return
    */
  def doFileUpload(uploadArgs: MUploadTargetQs) = csrf.Check {
    val bp = _uploadFileBp(uploadArgs.fileSizeB + 10000L)
    canUploadFile(uploadArgs).async(bp) { implicit request =>

      lazy val logPrefix = s"doUpload()#${System.currentTimeMillis()}:"
      LOGGER.trace(s"$logPrefix $uploadArgs <- ${request.path}")

      // TODO Сверить mime-тип файла.
      // TODO Проверить размер файла
      // TODO Сверить чек-суммы файла.
      // TODO Если изображение, то переместить файл в MLocalImg.
      // TODO Запустить анализ цветов, если тут картинка и задан ctxId.
      // TODO Заливка в хранилище (storage), если требуется.
      ???
    }
  }

}
