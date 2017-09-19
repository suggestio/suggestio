package io.suggest.ad.edit.m.edit

import diode.FastEq
import io.suggest.jd.MJdEditEdge
import io.suggest.jd.tags.IDocTag
import io.suggest.model.n2.edge.EdgeUid_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.17 19:05
  * Description: Модель состояния для контроллера PictureAh.
  */
object MPictureAh {

  /** Поддержка FastEq для [[MPictureAh]]. */
  implicit object MPictureAhFastEq extends FastEq[MPictureAh] {
    override def eqv(a: MPictureAh, b: MPictureAh): Boolean = {
      (a.files eq b.files) &&
        (a.edges eq b.edges) &&
        (a.selectedTag eq b.selectedTag)
    }
  }

}


/** Класс-контейнер данных для [[io.suggest.ad.edit.c.PictureAh]].
  *
  * @param files Карта данных по файлам.
  * @param edges Карта эджей.
  * @param selectedTag Текущий тег.
  */
case class MPictureAh(
                       files          : Map[EdgeUid_t, MFileInfo],
                       edges          : Map[EdgeUid_t, MJdEditEdge],
                       selectedTag    : Option[IDocTag]
                     ) {

  def withFiles(files: Map[EdgeUid_t, MFileInfo]) = copy(files = files)
  def withEdges(edges: Map[EdgeUid_t, MJdEditEdge]) = copy(edges = edges)
  def withSelectedTag(selectedTag: Option[IDocTag]) = copy(selectedTag = selectedTag)

}
