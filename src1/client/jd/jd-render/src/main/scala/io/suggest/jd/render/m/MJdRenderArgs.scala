package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.jd.MJdEditEdge
import io.suggest.model.n2.edge.EdgeUid_t
import japgolly.scalajs.react.vdom.TagMod
import japgolly.univeq.UnivEq
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.ReactUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 16:03
  * Description: Модель-контейнер данных для рендера одного JSON-документа.
  */

object MJdRenderArgs {

  implicit object MJdRenderArgsFastEq extends FastEq[MJdRenderArgs] {
    override def eqv(a: MJdRenderArgs, b: MJdRenderArgs): Boolean = {
      (a.edges ===* b.edges) &&
        (a.selJdtBgImgMod ===* b.selJdtBgImgMod)
    }
  }

  implicit def univEq: UnivEq[MJdRenderArgs] = UnivEq.derive

}


/** Класс-контейнер обобщённых данных для реднера JSON-документа.
  *
  * @param edges Карта данных по эджам, с сервера.
  * @param selJdtBgImgMod Трансформировать bgImg у выбранного тега с помощью этого TagMod.
  *                       Появилась для возможности заглядывать "под" изображение в редакторе, чтобы увидеть фон.
  */
case class MJdRenderArgs(
                          edges               : Map[EdgeUid_t, MJdEditEdge],
                          selJdtBgImgMod      : Option[TagMod]              = None
                        ) {

  def withEdges(edges: Map[EdgeUid_t, MJdEditEdge]) = copy(edges = edges)
  def withSelJdtBgImgMod(selJdtBgImgMod: Option[TagMod]) = copy(selJdtBgImgMod = selJdtBgImgMod)

}
