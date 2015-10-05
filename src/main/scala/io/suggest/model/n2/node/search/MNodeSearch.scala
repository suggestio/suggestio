package io.suggest.model.n2.node.search

import io.suggest.model.n2.edge.search.{EdgeSearchWrap, EdgeSearchDflt, EdgeSearch}
import io.suggest.model.n2.extra.search.{ExtrasSearchWrap, ExtrasSearchDflt, ExtrasSearch}
import io.suggest.model.n2.tag
import io.suggest.model.search._
import io.suggest.util.MacroLogsImplLazy

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.09.15 15:56
 * Description: Поисковые трейты для dynSearch по [[io.suggest.model.n2.node.MNode]] закидываются сюда.
 */
trait MNodeSearch
  extends Limit
  with Offset
  with EdgeSearch
  with ExtrasSearch
  with tag.vertex.search.FaceTextQuery
  with NodeTypes


/** Объединенные дефолтовые реализация поисковых критериев [[MNodeSearch]]. */
trait MNodeSearchDflt
  extends MNodeSearch
  with LimitDflt
  with OffsetDflt
  with EdgeSearchDflt
  with ExtrasSearchDflt
  with tag.vertex.search.FaceTextQueryDflt
  with NodeTypesDflt

/** Дефолтовая реализация [[MNodeSearchDflt]]. Упрощает жизнь компилятору. */
class MNodeSearchDfltImpl
  extends MNodeSearchDflt
  with MacroLogsImplLazy


/** Wrapper-реализация поисковых критериев [[MNodeSearch]] узла. */
trait MNodeSearchWrap
  extends MNodeSearch
  with LimitWrap
  with OffsetWrap
  with EdgeSearchWrap
  with ExtrasSearchWrap
  with tag.vertex.search.FaceTextQueryWrap
  with NodeTypesWrap
{
  override type WT <: MNodeSearch
}
