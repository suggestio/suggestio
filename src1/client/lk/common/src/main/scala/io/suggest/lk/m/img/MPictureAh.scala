package io.suggest.lk.m.img

import diode.FastEq
import io.suggest.color.MHistogram
import io.suggest.common.geom.d2.ISize2di
import io.suggest.img.MImgEdgeWithOps
import io.suggest.lk.m.MErrorPopupS
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.n2.edge.MEdgeDataJs
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
      (a.edges ===* b.edges) &&
        (a.imgEdgeId ===* b.imgEdgeId) &&
        (a.errorPopup ===* b.errorPopup) &&
        (a.cropPopup ===* b.cropPopup) &&
        (a.histograms ===* b.histograms) &&
        (a.cropContSz ===* b.cropContSz)
    }
  }

  implicit def univEq: UnivEq[MPictureAh] = UnivEq.derive

}


/** Класс-контейнер данных для [[io.suggest.lk.c.PictureAh]].
  *
  * @param edges Карта эджей с возможными данными по связанным файлам.
  * @param selectedTag Текущий тег.
  * @param errorPopup Код сообщения о какой-то ошибке, связанной с картинками.
  *                   Например, когда файл не является картинкой.
  * @param cropContSz Размер контейнера при активном кропе. Read-only.
  */
case class MPictureAh(
                       edges          : Map[EdgeUid_t, MEdgeDataJs],
                       imgEdgeId      : Option[MImgEdgeWithOps],
                       errorPopup     : Option[MErrorPopupS],
                       cropPopup      : Option[MPictureCropPopup],
                       histograms     : Map[String, MHistogram],
                       cropContSz     : Option[ISize2di],
                     ) {

  def withEdges(edges: Map[EdgeUid_t, MEdgeDataJs])             = copy(edges = edges)
  def withImgEdgeId(imgEdgeId: Option[MImgEdgeWithOps])         = copy(imgEdgeId = imgEdgeId)
  def withErrorPopup(errorPopup: Option[MErrorPopupS])          = copy(errorPopup = errorPopup)
  def withCropPopup(cropPopup: Option[MPictureCropPopup])       = copy(cropPopup = cropPopup)
  def withHistograms(histograms: Map[String, MHistogram])       = copy(histograms = histograms)

}
