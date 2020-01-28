package io.suggest.lk.m.img

import diode.FastEq
import io.suggest.color.MHistogram
import io.suggest.lk.m.MErrorPopupS
import io.suggest.n2.edge.EdgeUid_t
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.17 19:05
  * Description: Модель состояния для контроллера PictureAh.
  */
object MUploadAh {

  implicit def MPictureAhFastEq[V <: AnyRef : UnivEq]: FastEq[MUploadAh[V]] = {
    new FastEq[MUploadAh[V]] {
      override def eqv(a: MUploadAh[V], b: MUploadAh[V]): Boolean = {
        (a.edges ===* b.edges) &&
        (a.view ===* b.view) &&
        (a.errorPopup ===* b.errorPopup) &&
        (a.cropPopup ===* b.cropPopup) &&
        (a.histograms ===* b.histograms) &&
        (a.uploadExtra ===* b.uploadExtra)
      }
    }
  }

  @inline implicit def univEq[V: UnivEq]: UnivEq[MUploadAh[V]] = UnivEq.derive

}


/** Класс-контейнер данных для [[io.suggest.lk.c.UploadAh]].
  *
  * @param edges Карта эджей с возможными данными по связанным файлам.
  * @param errorPopup Код сообщения о какой-то ошибке, связанной с картинками.
  *                   Например, когда файл не является картинкой.
  */
case class MUploadAh[V](
                          edges          : Map[EdgeUid_t, MEdgeDataJs],
                          view           : V,
                          errorPopup     : Option[MErrorPopupS],
                          cropPopup      : Option[MPictureCropPopup],
                          histograms     : Map[String, MHistogram],
                          uploadExtra    : Option[String]               = None,
                        ) {

  def withEdges(edges: Map[EdgeUid_t, MEdgeDataJs])             = copy(edges = edges)
  def withView(view: V)                                         = copy(view = view)
  def withErrorPopup(errorPopup: Option[MErrorPopupS])          = copy(errorPopup = errorPopup)
  def withCropPopup(cropPopup: Option[MPictureCropPopup])       = copy(cropPopup = cropPopup)
  def withHistograms(histograms: Map[String, MHistogram])       = copy(histograms = histograms)
  def withUploadExtra(uploadExtra: Option[String]) = copy( uploadExtra = uploadExtra )

}
