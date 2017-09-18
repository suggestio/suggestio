package io.suggest.jd.tags.qd

import io.suggest.jd.MJdEditEdge
import io.suggest.model.n2.edge.EdgeUid_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.09.17 21:56
  * Description: Кросс-платформенная утиль для qd-моделей.
  * js-only утиль живёт в QuillDeltaJsUtil.
  */
object QdUtil {

  /** Является ли qd-дельта пустой?
    * Метод анализирует .ops на предмет присутствия хоть какого-то видимого текста.
    *
    * Возможно, такой агрессивный режим приведёт к невозможности использовать закрашенные
    * пробелы для рисования прямоугольников поверх фона.
    */
  def isEmpty(qd: QdTag, edgesMap: Map[EdgeUid_t, MJdEditEdge]): Boolean = {
    // Обычно, пустая дельта выглядит так: {"ops":[{"insert":"\n"}]}
    // Но мы будем анализировать весь список: допускаем целый список инзертов с итоговым пустым текстом.
    qd.ops.isEmpty || {
      val re = "\\s*".r.pattern
      qd.ops
        .iterator
        .flatMap(_.edgeInfo)
        .flatMap { ei =>
          edgesMap.get(ei.edgeUid)
        }
        .flatMap(_.text)
        // Допускаем, что любая пустая дельта может состоять из прозрачного мусора.
        .forall( re.matcher(_).matches() )
    }
  }

}
