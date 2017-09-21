package io.suggest.ad.edit.m.edit.pic

import diode.FastEq
import io.suggest.ad.edit.m.pop.MPictureCropPopup
import io.suggest.file.MJsFileInfo
import io.suggest.i18n.MMessage
import io.suggest.jd.MJdEditEdge
import io.suggest.jd.tags.IDocTag
import io.suggest.lk.m.MErrorPopupS
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

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
      (a.files ===* b.files) &&
        (a.edges ===* b.edges) &&
        (a.selectedTag ===* b.selectedTag) &&
        (a.errorPopup ===* b.errorPopup) &&
        (a.cropPopup ===* b.cropPopup)
    }
  }

  implicit def univEq: UnivEq[MPictureAh] = UnivEq.derive

}


/** Класс-контейнер данных для [[io.suggest.ad.edit.c.PictureAh]].
  *
  * @param files Карта данных по файлам.
  * @param edges Карта эджей.
  * @param selectedTag Текущий тег.
  * @param errorPopup Код сообщения о какой-то ошибке, связанной с картинками.
  *                   Например, когда файл не является картинкой.
  */
case class MPictureAh(
                       files          : Map[EdgeUid_t, MJsFileInfo],
                       edges          : Map[EdgeUid_t, MJdEditEdge],
                       selectedTag    : Option[IDocTag],
                       errorPopup     : Option[MErrorPopupS],
                       cropPopup      : Option[MPictureCropPopup]
                     ) {

  def withFiles(files: Map[EdgeUid_t, MJsFileInfo])   = copy(files = files)
  def withEdges(edges: Map[EdgeUid_t, MJdEditEdge]) = copy(edges = edges)
  def withSelectedTag(selectedTag: Option[IDocTag]) = copy(selectedTag = selectedTag)
  def withErrorPopup(errorPopup: Option[MErrorPopupS])          = copy(errorPopup = errorPopup)
  def withCropPopup(cropPopup: Option[MPictureCropPopup])   = copy(cropPopup = cropPopup)

}
