package io.suggest.lk.m.img

import diode.FastEq
import io.suggest.color.MHistogram
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

  implicit def MPictureAhFastEq[V <: AnyRef : UnivEq]: FastEq[MPictureAh[V]] = {
    new FastEq[MPictureAh[V]] {
      override def eqv(a: MPictureAh[V], b: MPictureAh[V]): Boolean = {
        (a.edges ===* b.edges) &&
        (a.view ===* b.view) &&
        (a.errorPopup ===* b.errorPopup) &&
        (a.cropPopup ===* b.cropPopup) &&
        (a.histograms ===* b.histograms)
      }
    }
  }

  @inline implicit def univEq[V: UnivEq]: UnivEq[MPictureAh[V]] = UnivEq.derive

}


/** Класс-контейнер данных для [[io.suggest.lk.c.PictureAh]].
  *
  * @param edges Карта эджей с возможными данными по связанным файлам.
  * @param selectedTag Текущий тег.
  * @param errorPopup Код сообщения о какой-то ошибке, связанной с картинками.
  *                   Например, когда файл не является картинкой.
  */
case class MPictureAh[V](
                          edges          : Map[EdgeUid_t, MEdgeDataJs],
                          view           : V,
                          errorPopup     : Option[MErrorPopupS],
                          cropPopup      : Option[MPictureCropPopup],
                          histograms     : Map[String, MHistogram]
                        ) {

  def withEdges(edges: Map[EdgeUid_t, MEdgeDataJs])             = copy(edges = edges)
  def withView(view: V)                                         = copy(view = view)
  def withErrorPopup(errorPopup: Option[MErrorPopupS])          = copy(errorPopup = errorPopup)
  def withCropPopup(cropPopup: Option[MPictureCropPopup])       = copy(cropPopup = cropPopup)
  def withHistograms(histograms: Map[String, MHistogram])       = copy(histograms = histograms)

}
