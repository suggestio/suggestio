package io.suggest.model.n2.search

import io.suggest.model.n2.tag.vertex.search._
import io.suggest.model.search._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.09.15 15:56
 * Description: Поисковые трейты для dynSearch по [[io.suggest.model.n2.MNode]] закидываются сюда.
 */
trait MNodeSearch
  extends Limit
  with Offset
  with TagVertexFaceTextMatch


/** Объединенные дефолтовые реализация поисковых критериев [[MNodeSearch]]. */
trait MNodeSearchDflt
  extends MNodeSearch
  with LimitDflt
  with OffsetDflt
  with TagVertexFaceTextMatchDflt
/** Дефолтовая реализация [[MNodeSearchDflt]]. Упрощает жизнь компилятору. */
class MNodeSearchDfltImpl
  extends MNodeSearchDflt


/** Wrapper-реализация поисковых критериев [[MNodeSearch]] узла. */
trait MNodeSearchWrap
  extends MNodeSearch
  with LimitWrap
  with OffsetWrap
  with TagVertexFaceTextMatchWrap
{
  override type WT <: MNodeSearch
}
