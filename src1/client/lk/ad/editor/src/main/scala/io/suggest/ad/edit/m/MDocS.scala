package io.suggest.ad.edit.m

import io.suggest.jd.MJdEditEdge
import io.suggest.jd.render.m.IJdFocRenderData
import io.suggest.jd.tags.JsonDocument

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 18:33
  * Description: Модель состояния документа в редакторе.
  */
case class MDocS(
                  override val template  : JsonDocument,
                  override val edges     : Map[Int, MJdEditEdge]
                )
  extends IJdFocRenderData
{

  def withTemplate(template: JsonDocument) = copy(template = template)
  def withEdges(edges: Map[Int, MJdEditEdge]) = copy(edges = edges)


  /** Выдать экземпляр данных для рендера json-документа, т.е. контента рекламной карточки. */
  def jdRenderData: IJdFocRenderData = this

}
