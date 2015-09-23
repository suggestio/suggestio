package io.suggest.model.n2.node

import io.suggest.model.n2.tag
import io.suggest.model.search._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.09.15 15:56
 * Description: Поисковые трейты для dynSearch по [[MNode]] закидываются сюда.
 */
trait MNodeSearch
  extends Limit
  with Offset
  with tag.vertex.search.FaceTextQuery


/** Объединенные дефолтовые реализация поисковых критериев [[MNodeSearch]]. */
trait MNodeSearchDflt
  extends MNodeSearch
  with LimitDflt
  with OffsetDflt
  with tag.vertex.search.FaceTextQueryDflt

/** Дефолтовая реализация [[MNodeSearchDflt]]. Упрощает жизнь компилятору. */
class MNodeSearchDfltImpl
  extends MNodeSearchDflt


/** Wrapper-реализация поисковых критериев [[MNodeSearch]] узла. */
trait MNodeSearchWrap
  extends MNodeSearch
  with LimitWrap
  with OffsetWrap
  with tag.vertex.search.FaceTextQueryWrap
{
  override type WT <: MNodeSearch
}
