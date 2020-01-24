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
  }

  implicit def uploadInfoQsb(implicit
                             fileHandlerOptB    : QueryStringBindable[Option[MUploadFileHandler]],
                             cdArgsOptB         : QueryStringBindable[Option[MColorDetectArgs]],
                             nodeTypeOptB       : QueryStringBindable[Option[MNodeType]],
                             strOptB            : QueryStringBindable[Option[String]],
                            ): QueryStringBindable[MUploadInfoQs] = {
    new QueryStringBindableImpl[MUploadInfoQs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MUploadInfoQs]] = {
        val F = Fields
        val k = key1F(key)
        for {
          fileHandlerOptE   <- fileHandlerOptB.bind   ( k(F.FILE_HANDLER_FN),   params )
          colorDetectE      <- cdArgsOptB.bind        ( k(F.COLOR_DETECT_FN),   params )
          nodeTypeOptE      <- nodeTypeOptB.bind      ( k(F.NODE_TYPE_FN),      params )
          nodeIdOptE        <- strOptB.bind           ( k(F.EXIST_NODE_ID_FN),  params )
        } yield {
          for {
            fileHandlerOpt  <- fileHandlerOptE
            colorDetect     <- colorDetectE
            nodeTypeOpt     <- nodeTypeOptE
            existNodeIdOpt  <- nodeIdOptE
          } yield {
            MUploadInfoQs(
              fileHandler   = fileHandlerOpt,
              colorDetect   = colorDetect,
              nodeType      = nodeTypeOpt,
              existNodeId   = existNodeIdOpt
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
  */
case class MUploadInfoQs(
                          nodeType          : Option[MNodeType],
                          fileHandler       : Option[MUploadFileHandler],
                          colorDetect       : Option[MColorDetectArgs],
                          existNodeId       : Option[String]              = None,
                        )
