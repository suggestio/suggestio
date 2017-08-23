package io.suggest.ad.edit.m

import io.suggest.jd.MJdEditEdge
import io.suggest.jd.tags.JsonDocument

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 18:33
  * Description: Модель состояния документа в редакторе.
  */
case class MDocS(
                  template  : JsonDocument,
                  edges     : Map[Int, MJdEditEdge]
                ) {

  def withTemplate(template: JsonDocument) = copy(template = template)
  def withEdges(edges: Map[Int, MJdEditEdge]) = copy(edges = edges)

}
