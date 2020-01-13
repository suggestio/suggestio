package models.mup

import io.suggest.file.up.MFile4UpProps
import io.suggest.n2.media.storage.MAssignedStorage
import io.suggest.n2.node.MNodeType
import io.suggest.sec.QsbSigner
import io.suggest.sec.m.SecretKeyInit
import io.suggest.xplay.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.10.17 15:13
  * Description: URL-qs-модель, защищённая подписью, для ссылки аплоада файла на сервер.
  */

object MUploadTargetQs extends SecretKeyInit {

  /** Имена полей модели. */
  object Fields {

    val FILE_PROPS_FN     = "f"
    val FILE_HANDLER_FN   = "d"
    val PERSON_ID_FN      = "p"
    val VALID_TILL_FN     = "c"
    val STORAGE_FN        = "s"
    val COLOR_DETECT_FN   = "l"
    val NODE_TYPE_FN      = "n"

    val SIGNATURE_FN      = "z"

  }

  override def CONF_KEY = "upload.url.sign.secret"
  private var SIGN_SECRET: String = _
  override def setSignSecret(secretKey: String): Unit = {
    SIGN_SECRET = secretKey
  }

  /** Поддержка QueryStringBindable. */
  implicit def uploadTargetQsQsb(implicit
                                 file4UpPropsB      : QueryStringBindable[MFile4UpProps],
                                 longB              : QueryStringBindable[Long],
                                 fileHandlerOptB    : QueryStringBindable[Option[MUploadFileHandler]],
                                 assignedStorageB   : QueryStringBindable[MAssignedStorage],
                                 strOptB            : QueryStringBindable[Option[String]],
                                 cdArgsOptB         : QueryStringBindable[Option[MColorDetectArgs]],
                                 nodeTypeB          : QueryStringBindable[MNodeType],
                                ): QueryStringBindable[MUploadTargetQs] = {
    new QueryStringBindableImpl[MUploadTargetQs] {
      def getQsbSigner(key: String) = new QsbSigner(SIGN_SECRET, Fields.SIGNATURE_FN)

      /** Биндинг значения [[MUploadTargetQs]] из URL qs. */
      override def bind(key: String, params0: Map[String, Seq[String]]): Option[Either[String, MUploadTargetQs]] = {
        val F = Fields
        val k = key1F(key)
        for {
          params            <- getQsbSigner(key).signedOrNone(k(""), params0)
          filePropsE        <- file4UpPropsB.bind     ( k(F.FILE_PROPS_FN),   params )
          fileHandlerOptE   <- fileHandlerOptB.bind   ( k(F.FILE_HANDLER_FN), params )
          personIdOptE      <- strOptB.bind           ( k(F.PERSON_ID_FN),    params )
          validTillE        <- longB.bind             ( k(F.VALID_TILL_FN),   params )
          storageE          <- assignedStorageB.bind  ( k(F.STORAGE_FN),      params )
          colorDetectE      <- cdArgsOptB.bind        ( k(F.COLOR_DETECT_FN), params )
          nodeTypeE         <- nodeTypeB.bind         ( k(F.NODE_TYPE_FN),    params )
        } yield {
          for {
            fileProps       <- filePropsE
            fileHandlerOpt  <- fileHandlerOptE
            personIdOpt     <- personIdOptE
            validTill       <- validTillE
            storage         <- storageE
            colorDetect     <- colorDetectE
            nodeType        <- nodeTypeE
          } yield {
            MUploadTargetQs(
              fileProps     = fileProps,
              fileHandler   = fileHandlerOpt,
              personId      = personIdOpt,
              validTillS    = validTill,
              storage       = storage,
              colorDetect   = colorDetect,
              nodeType      = nodeType,
            )
          }
        }
      }       // bind()


      /** Сериализация полей [[MUploadTargetQs]] в строку URL qs. */
      override def unbind(key: String, value: MUploadTargetQs): String = {
        val F = Fields
        val k = key1F(key)
        val unsigned = _mergeUnbinded1(
          file4UpPropsB.unbind      ( k(F.FILE_PROPS_FN),       value.fileProps   ),
          fileHandlerOptB.unbind    ( k(F.FILE_HANDLER_FN),     value.fileHandler ),
          strOptB.unbind            ( k(F.PERSON_ID_FN),        value.personId    ),
          longB.unbind              ( k(F.VALID_TILL_FN),       value.validTillS  ),
          assignedStorageB.unbind   ( k(F.STORAGE_FN),          value.storage     ),
          cdArgsOptB.unbind         ( k(F.COLOR_DETECT_FN),     value.colorDetect ),
          nodeTypeB.unbind          ( k(F.NODE_TYPE_FN),        value.nodeType    ),
        )
        // Подписать это всё.
        getQsbSigner(key)
          .mkSigned(key, unsigned)
      }     // unbind()

    }       // new QSBImpl
  }         // implicit def qsb

}


/** Класс модели для заливки данных на сервер.
  *
  * @param fileProps Инстанс MFile4UpProps (с первой фазы аплоада).
  * @param fileHandler Опциональный режим перехвата файла в контроллере, чтобы вместо /tmp/... сразу сохранять в иное место.
  * @param personId id юзера.
  * @param validTillS TTL. Вычисляется как currentTimeMillis/1000 + TTL в момент генерации ссылки (в секундах).

  * @param colorDetect Запустить MainColorDetector после.
  *                    Значение -- кол-во цветов, которые надо отправить на клиент.
  * @param nodeType Тип создаваемого узла.
  */
case class MUploadTargetQs(
                            fileProps   : MFile4UpProps,
                            fileHandler : Option[MUploadFileHandler],
                            personId    : Option[String],
                            validTillS  : Long,
                            storage     : MAssignedStorage,
                            colorDetect : Option[MColorDetectArgs],
                            nodeType    : MNodeType,
                          )
