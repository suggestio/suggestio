package models.mup

import io.suggest.n2.media.MFileMeta
import io.suggest.n2.media.storage.MAssignedStorage
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
    val PERSON_ID_FN      = "p"
    val VALID_TILL_FN     = "c"
    val STORAGE_FN        = "s"
    val INFO_FN           = "i"

    val SIGNATURE_FN      = "z"

  }

  override def CONF_KEY = "upload.url.sign.secret"
  // Доступ для mup.*, т.к. download тоже использует этот ключ для упрощения.
  private[mup] var SIGN_SECRET: String = _
  override def setSignSecret(secretKey: String): Unit = {
    SIGN_SECRET = secretKey
  }

  /** Поддержка QueryStringBindable. */
  implicit def uploadTargetQsQsb(implicit
                                 file4UpPropsB      : QueryStringBindable[MFileMeta],
                                 longB              : QueryStringBindable[Long],
                                 assignedStorageB   : QueryStringBindable[MAssignedStorage],
                                 strOptB            : QueryStringBindable[Option[String]],
                                 upInfoB            : QueryStringBindable[MUploadInfoQs],
                                ): QueryStringBindable[MUploadTargetQs] = {
    new QueryStringBindableImpl[MUploadTargetQs] {
      private def qsbSigner = new QsbSigner(SIGN_SECRET, Fields.SIGNATURE_FN)

      /** Биндинг значения [[MUploadTargetQs]] из URL qs. */
      override def bind(key: String, params0: Map[String, Seq[String]]): Option[Either[String, MUploadTargetQs]] = {
        val F = Fields
        val k = key1F(key)
        for {
          params            <- qsbSigner.signedOrNone(k(""), params0)
          filePropsE        <- file4UpPropsB.bind     ( k(F.FILE_PROPS_FN),   params )
          personIdOptE      <- strOptB.bind           ( k(F.PERSON_ID_FN),    params )
          validTillE        <- longB.bind             ( k(F.VALID_TILL_FN),   params )
          storageE          <- assignedStorageB.bind  ( k(F.STORAGE_FN),      params )
          upInfoE           <- upInfoB.bind           ( k(F.INFO_FN),         params )
        } yield {
          for {
            fileProps       <- filePropsE
            personIdOpt     <- personIdOptE
            validTill       <- validTillE
            storage         <- storageE
            upInfo          <- upInfoE
          } yield {
            MUploadTargetQs(
              fileProps     = fileProps,
              personId      = personIdOpt,
              validTillS    = validTill,
              storage       = storage,
              info          = upInfo,
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
          strOptB.unbind            ( k(F.PERSON_ID_FN),        value.personId    ),
          longB.unbind              ( k(F.VALID_TILL_FN),       value.validTillS  ),
          assignedStorageB.unbind   ( k(F.STORAGE_FN),          value.storage     ),
          upInfoB.unbind            ( k(F.INFO_FN),             value.info        ),
        )
        // Подписать это всё.
        qsbSigner.mkSigned(key, unsigned)
      }     // unbind()

    }       // new QSBImpl
  }         // implicit def qsb

}


/** Класс модели для заливки данных на сервер.
  *
  * @param fileProps Инстанс MFile4UpProps (с первой фазы аплоада).
  * @param personId id юзера.
  * @param validTillS TTL. Вычисляется как currentTimeMillis/1000 + TTL в момент генерации ссылки (в секундах).
  * @param info Инфа, целиком пробрасываемая во вторую фазу заливки.
  */
case class MUploadTargetQs(
                            fileProps   : MFileMeta,
                            personId    : Option[String],
                            validTillS  : Long,
                            storage     : MAssignedStorage,
                            info        : MUploadInfoQs,
                          )
