package io.suggest.model.n2.node.search

import io.suggest.model.n2.edge.search._
import io.suggest.model.n2.extra.search._
import io.suggest.model.n2.geo.search._
import io.suggest.model.n2.node.common.search._
import io.suggest.model.n2.node.meta.search._
import io.suggest.model.n2.tag.vertex.search._
import io.suggest.model.search._
import io.suggest.util.MacroLogsImplLazy

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.09.15 15:56
 * Description: Поисковые трейты для dynSearch по [[io.suggest.model.n2.node.MNode]] закидываются сюда.
 */
trait MNodeSearch
  extends FtsAll
  with WithIds
  with GeoShapeIntersect
  with OutEdges
  with ShownTypeId
  with AdnRights
  with AdnIsTest
  with AdnSinks
  with FaceTextQuery
  with NodeTypes
  with WithoutIds
  with ShowInScNl
  with IsEnabled
  with IsDependent
  with GeoDstSort
  with NameSort
  with RandomSort
  with Limit
  with Offset

/** Реализация [[MNodeSearch]] для упрошения жизни компилятору. */
abstract class MNodeSearchImpl
  extends MNodeSearch


/** Объединенные дефолтовые реализация поисковых критериев [[MNodeSearch]]. */
trait MNodeSearchDflt
  extends MNodeSearch
  with FtsAllDflt
  with WithIdsDflt
  with GeoShapeIntersectDflt
  with OutEdgesDflt
  with ShownTypeIdDflt
  with AdnRightsDflt
  with AdnIsTestDflt
  with AdnSinksDflt
  with FaceTextQueryDflt
  with NodeTypesDflt
  with WithoutIdsDflt
  with ShowInScNlDflt
  with IsEnabledDflt
  with IsDependentDflt
  with GeoDstSortDflt
  with NameSortDflt
  with RandomSortDflt
  with LimitDflt
  with OffsetDflt

/** Дефолтовая реализация [[MNodeSearchDflt]].
  * Упрощает жизнь компилятору при сборке недефолтовых классов-реализаций. */
class MNodeSearchDfltImpl
  extends MNodeSearchImpl
  with MNodeSearchDflt
  with MacroLogsImplLazy


/** Wrapper-реализация поисковых критериев [[MNodeSearch]] узла. */
trait MNodeSearchWrap
  extends MNodeSearch
  with FtsAllWrap
  with WithIdsWrap
  with GeoShapeIntersectWrap
  with OutEdgesWrap
  with ShownTypeIdWrap
  with AdnRightsWrap
  with AdnIsTestWrap
  with AdnSinksWrap
  with FaceTextQueryWrap
  with NodeTypesWrap
  with WithoutIdsWrap
  with ShowInScNlWrap
  with IsEnabledWrap
  with IsDependentWrap
  with GeoDstSortWrap
  with NameSortWrap
  with RandomSortWrap
  with LimitWrap
  with OffsetWrap
{
  override type WT <: MNodeSearch
}

/** Реализация [[MNodeSearchWrap]] для упрощения жизни компиляторам. */
abstract class MNodeSearchWrapImpl_
  extends MNodeSearchImpl
  with MNodeSearchWrap
  with MacroLogsImplLazy
{
  override type WT <: MNodeSearch
}


abstract class MNodeSearchWrapImpl
  extends MNodeSearchWrapImpl_
{
  override type WT = MNodeSearch
}
