package io.suggest.jd.render.m

import io.suggest.jd.MJdEditEdge
import io.suggest.model.n2.edge.EdgeUid_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 16:03
  * Description: Модель-контейнер данных для рендера одного JSON-документа.
  */


/** Класс-контейнер обобщённых данных для реднера JSON-документа.
  *
  * @param edges Карта данных по эджам, с сервера.
  */
case class MJdRenderArgs(
                          edges     : Map[EdgeUid_t, MJdEditEdge]
                        ) {

  def withEdges(edges: Map[Int, MJdEditEdge]) = copy(edges = edges)

}