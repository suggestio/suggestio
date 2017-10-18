package io.suggest.jd.tags.qd

import io.suggest.jd.tags.JdTag
import io.suggest.jd.tags.JdTag.Implicits._
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.n2.edge.MEdgeDataJs

import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.09.17 21:56
  * Description: Кросс-платформенная утиль для qd-моделей.
  * js-only утиль живёт в QuillDeltaJsUtil.
  */
object QdJsUtil {

  /** Паттерн полной строки, которая считается пустой. */
  private val EMPTY_TEXT_PATTERN_RE = "\\s*".r.pattern

  /** Является ли qd-дельта пустой?
    * Метод анализирует .ops на предмет присутствия хоть какого-то видимого текста.
    *
    * Возможно, такой агрессивный режим приведёт к невозможности использовать закрашенные
    * пробелы для рисования прямоугольников поверх фона.
    */
  def isEmpty(qdTree: Tree[JdTag], edgesMap: Map[EdgeUid_t, MEdgeDataJs]): Boolean = {
    // Обычно, пустая дельта выглядит так: {"ops":[{"insert":"\n"}]}
    // Но мы будем анализировать весь список: допускаем целый список инзертов с итоговым пустым текстом.
    qdTree.qdOpsIter.isEmpty || {
      qdTree
        .deepEdgesUidsIter
        .flatMap { edgesMap.get }
        // Допускаем, что любая пустая дельта может состоять из прозрачного мусора.
        .forall( isEdgeDataEmpty )
    }
  }

  /** Считать ли указанный data-эдж пустым?
    * @return Да, если полезной информации в нём не обнаружено.
    */
  def isEdgeDataEmpty(e: MEdgeDataJs): Boolean = {
    e.fileJs.isEmpty &&
      e.jdEdge.url.isEmpty &&
      e.jdEdge.fileSrv.isEmpty && {
        e.jdEdge.text
         .fold(true){ EMPTY_TEXT_PATTERN_RE.matcher(_).matches() }
      }
  }

}
