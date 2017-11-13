package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.n2.edge.MEdgeDataJs
import japgolly.scalajs.react.vdom.TagMod
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.ReactUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 16:03
  * Description: Модель-контейнер данных для рендера одного JSON-документа.
  */

object MJdRenderArgs {

  /** Поддержка FastEq для инстансов [[MJdRenderArgs]]. */
  implicit object MJdRenderArgsFastEq extends FastEq[MJdRenderArgs] {
    override def eqv(a: MJdRenderArgs, b: MJdRenderArgs): Boolean = {
      (a.edges ===* b.edges) &&
        (a.selJdtBgImgMod ===* b.selJdtBgImgMod) &&
        (a.hideNonMainStrips ==* b.hideNonMainStrips)
    }
  }

  implicit def univEq: UnivEq[MJdRenderArgs] = UnivEq.derive

}


/** Класс-контейнер обобщённых данных для реднера JSON-документа.
  *
  * @param edges Карта данных по эджам, с сервера.
  * @param selJdtBgImgMod Трансформировать bgImg у выбранного тега с помощью этого TagMod.
  *                       Появилась для возможности заглядывать "под" изображение в редакторе, чтобы увидеть фон.
  * @param hideNonMainStrips Скрыть все неглавные стрипы? Раньше была прозрачность, но она конфликтует с grid'ом.
  */
case class MJdRenderArgs(
                          edges               : Map[EdgeUid_t, MEdgeDataJs],
                          selJdtBgImgMod      : Option[TagMod]              = None,
                          hideNonMainStrips   : Boolean                     = false
                        ) {

  def withEdges(edges: Map[EdgeUid_t, MEdgeDataJs])             = copy(edges = edges)
  def withSelJdtBgImgMod(selJdtBgImgMod: Option[TagMod])        = copy(selJdtBgImgMod = selJdtBgImgMod)
  def withHideNonMainStrips(hideNonMainStrips: Boolean)         = copy(hideNonMainStrips = hideNonMainStrips)

}
