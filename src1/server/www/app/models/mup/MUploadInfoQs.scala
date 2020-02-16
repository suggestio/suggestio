package models.mup

import io.suggest.n2.node.MNodeType
import io.suggest.xplay.qsb.QueryStringBindableImpl
import japgolly.univeq.UnivEq
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.01.2020 16:48
  * Description: Контейнер данных Upload для второй фазы, которые собираются ДО первой фазы и пробрасываются
  * сквозь первую фазу в [[MUploadTargetQs]].
  */
object MUploadInfoQs {

  object Fields {
    val FILE_HANDLER_FN   = "d"
    val COLOR_DETECT_FN   = "l"
    val NODE_TYPE_FN      = "t"
    val EXIST_NODE_ID_FN  = "n"
    val SYSTEM_RESP_FN    = "S"
    val OBEY_MIME_FN      = "o"
  }

  implicit def uploadInfoQsb(implicit
                             fileHandlerOptB    : QueryStringBindable[Option[MUploadFileHandler]],
                             cdArgsOptB         : QueryStringBindable[Option[MColorDetectArgs]],
                             nodeTypeOptB       : QueryStringBindable[Option[MNodeType]],
                            ): QueryStringBindable[MUploadInfoQs] = {
    new QueryStringBindableImpl[MUploadInfoQs] {
      private lazy val strOptB = implicitly[QueryStringBindable[Option[String]]]
      private lazy val boolOptB = implicitly[QueryStringBindable[Option[Boolean]]]

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MUploadInfoQs]] = {
        val F = Fields
        val k = key1F(key)
        for {
          fileHandlerOptE   <- fileHandlerOptB.bind   ( k(F.FILE_HANDLER_FN),   params )
          colorDetectE      <- cdArgsOptB.bind        ( k(F.COLOR_DETECT_FN),   params )
          nodeTypeOptE      <- nodeTypeOptB.bind      ( k(F.NODE_TYPE_FN),      params )
          nodeIdOptE        <- strOptB.bind           ( k(F.EXIST_NODE_ID_FN),  params )
          systemRespOptE    <- boolOptB.bind          ( k(F.SYSTEM_RESP_FN),    params )
          obeyMimeOptE      <- boolOptB.bind          ( k(F.OBEY_MIME_FN),      params )
        } yield {
          for {
            fileHandlerOpt  <- fileHandlerOptE
            colorDetect     <- colorDetectE
            nodeTypeOpt     <- nodeTypeOptE
            existNodeIdOpt  <- nodeIdOptE
            systemRespOpt   <- systemRespOptE
            obeyMimeOpt     <- obeyMimeOptE
          } yield {
            MUploadInfoQs(
              fileHandler       = fileHandlerOpt,
              colorDetect       = colorDetect,
              nodeType          = nodeTypeOpt,
              existNodeId       = existNodeIdOpt,
              systemResp        = systemRespOpt,
              obeyMime          = obeyMimeOpt,
            )
          }
        }
      }

      override def unbind(key: String, value: MUploadInfoQs): String = {
        val F = Fields
        val k = key1F(key)
        _mergeUnbinded1(
          fileHandlerOptB.unbind    ( k(F.FILE_HANDLER_FN),     value.fileHandler ),
          cdArgsOptB.unbind         ( k(F.COLOR_DETECT_FN),     value.colorDetect ),
          nodeTypeOptB.unbind       ( k(F.NODE_TYPE_FN),        value.nodeType    ),
          strOptB.unbind            ( k(F.EXIST_NODE_ID_FN),    value.existNodeId ),
          boolOptB.unbind           ( k(F.SYSTEM_RESP_FN),      value.systemResp  ),
          boolOptB.unbind           ( k(F.OBEY_MIME_FN),        value.obeyMime    ),
        )
      }
    }
  }


  @inline implicit def univEq: UnivEq[MUploadInfoQs] = UnivEq.derive

}


/** Контрейнер данных инфы по заливке, которая пробрасывается одним куском во вторую фазу заливки.
  *
  * @param fileHandler Опциональный режим перехвата файла в контроллере, чтобы вместо /tmp/... сразу сохранять в иное место.
  * @param colorDetect Запустить MainColorDetector после.
  *                    Значение -- кол-во цветов, которые надо отправить на клиент.
  * @param nodeType Тип создаваемого узла.
  *                 Если existingNodeId задан, то можно None.
  * @param existNodeId id существующего узла: не создавать новый узел, а использовать существующий.
  * @param systemResp Ответ должен содержать в себе системные данные: метаданные серверного хранилища и т.д.
  * @param obeyMime Если mime-тип принятого файла не совпадает, то
  *                 true - брать MIME, присланный с клиента (определён браузером или как-то ещё).
  *                 false - брать MIME, определённый сервером.
  */
case class MUploadInfoQs(
                          nodeType          : Option[MNodeType],
                          fileHandler       : Option[MUploadFileHandler]  = None,
                          colorDetect       : Option[MColorDetectArgs]    = None,
                          existNodeId       : Option[String]              = None,
                          systemResp        : Option[Boolean]             = None,
                          obeyMime          : Option[Boolean]             = None,
                        )
