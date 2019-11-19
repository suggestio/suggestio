package io.suggest.ad.edit.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.ad.edit.m.JdDocChanged
import io.suggest.ad.edit.m.vld.MJdVldAh
import io.suggest.jd.{JdDocValidator, MEdgePicInfo, MJdEdgeVldInfo}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.10.17 14:33
  * Description: Контроллер живой валидации документа прямо в редакторе во время редактирования.
  *
  * Создан для ручной отладки валидации, потому что валидация -- это ресурсоёмкий процесс, и на продакшене неуместен.
  * Подключается параллельно к основной цепочке контроллеров, выполняя живую валидацию в фоне.
  */
class JdVldAh[M]( modelRW: ModelRW[M, MJdVldAh] ) extends ActionHandler(modelRW) {

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал об изменении в json-документе. Надо запустить пере-валидацию документа.
    case JdDocChanged =>
      val v0 = value

      val vld = new JdDocValidator(
        tolerant = false,
        edges = v0.jdData.edges
          .view
          .mapValues { eData =>
            MJdEdgeVldInfo(
              jdEdge = eData.jdEdge,
              img = for (fileJs <- eData.fileJs) yield {
                MEdgePicInfo(
                  isImg  = true,
                  imgWh  = fileJs.whPx,
                  // Формат картинки не важен на клиенте, но важен серверу.
                  dynFmt = None
                )
              }
            )
          }
          .toMap
      )
      val vldRes = vld.validateDocumentTree( v0.jdData.doc.template )
      println( vldRes )

      noChange

  }

}
