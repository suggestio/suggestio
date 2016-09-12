package io.suggest.model.n2.node.search

import com.typesafe.scalalogging.slf4j.Logger
import io.suggest.model.n2.edge.search._
import io.suggest.model.n2.extra.domain.{DomainsSearch, DomainsSearchDflt, DomainsSearchWrap}
import io.suggest.model.n2.extra.search._
import io.suggest.model.n2.geo.search._
import io.suggest.model.n2.node.common.search._
import io.suggest.model.n2.node.meta.search._
import io.suggest.model.search._
import io.suggest.util.{MacroLogsDyn, MacroLogsImpl, MacroLogsImplLazy}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.09.15 15:56
 * Description: Поисковые трейты для dynSearch по [[io.suggest.model.n2.node.MNode]] закидываются сюда.
 */
trait MNodeSearch
  extends FtsAll
  with WithIds
  with DomainsSearch
  with OutEdges
  with BleBeacons
  with ShownTypeId
  with AdnRights
  with AdnIsTest
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
  with DateCreatedSort
  with HasGeoPoint

/** Реализация [[MNodeSearch]] для упрошения жизни компилятору. */
abstract class MNodeSearchImpl
  extends MNodeSearch


/** Объединенные дефолтовые реализация поисковых критериев [[MNodeSearch]]. */
trait MNodeSearchDflt
  extends MNodeSearch
  with FtsAllDflt
  with WithIdsDflt
  with DomainsSearchDflt
  with OutEdgesDflt
  with BleBeaconsDflt
  with ShownTypeIdDflt
  with AdnRightsDflt
  with AdnIsTestDflt
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
  with DateCreatedSortDflt
  with HasGeoPointDflt


/** Объект-компаньон содержит рантаймовые статические константы для класса [[MNodeSearchDfltImpl]].
  * Изначально, он хранил в себе связанный логгер. */
object MNodeSearchDfltImpl extends MacroLogsImpl

/** Дефолтовая реализация [[MNodeSearchDflt]].
  * Упрощает жизнь компилятору при сборке недефолтовых классов-реализаций. */
class MNodeSearchDfltImpl
  extends MNodeSearchImpl
  with MNodeSearchDflt
{
  override def LOGGER = MNodeSearchDfltImpl.LOGGER
}


/** Wrapper-реализация поисковых критериев [[MNodeSearch]] узла. */
trait MNodeSearchWrap
  extends MNodeSearch
  with FtsAllWrap
  with WithIdsWrap
  with DomainsSearchWrap
  with OutEdgesWrap
  with BleBeaconsWrap
  with ShownTypeIdWrap
  with AdnRightsWrap
  with AdnIsTestWrap
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
  with DateCreatedSortWrap
  with HasGeoPointWrap
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
