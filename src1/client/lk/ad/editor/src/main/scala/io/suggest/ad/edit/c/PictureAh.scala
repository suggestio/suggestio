package io.suggest.ad.edit.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.ad.edit.m.PictureFileChanged
import io.suggest.ad.edit.m.edit.MPictureAh
import io.suggest.model.n2.edge.EdgeUid_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.17 19:03
  * Description: Контроллер управления картинками.
  */
class PictureAh[M](modelRW: ModelRW[M, MPictureAh]) extends ActionHandler(modelRW) {

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Выставлен файл в input'е заливки картинки.
    // 1. Отрендерить его на экране (т.е. сохранить в состоянии в виде блоба).
    // 2. Запустить фоновую закачку файла на сервер.
    case m: PictureFileChanged =>
      val v0 = value
      val selJdt = v0.selectedTag.get
      val selJdtEdgeUids = selJdt.deepEdgesUidsIter.toSet
      if (m.files.isEmpty) {
        // В теории, файл может быть удалён, т.е. список файлов изменился в []
        val deleteKeyF = { k: EdgeUid_t => !selJdtEdgeUids.contains(k) }
        val edges2 = v0.edges.filterKeys( deleteKeyF )
        val files2 = v0.files.filterKeys( deleteKeyF )
        val v2 = v0.copy(
          files = files2,
          edges = edges2,
          selectedTag = ???
        )
        ???
      } else {
        ???
      }
      ???

  }

}
