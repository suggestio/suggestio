package io.suggest.ym.model.common

import io.suggest.model.common._
import io.suggest.model.es.EsModelUtil
import io.suggest.model.search._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.08.14 14:26
 * Description: Аддон для модели MAdnNode, которая отвечает за сборку и исполнение поисковых запросов узлов рекламной
 * сети.
 */

/** Интерфейс для описания критериев того, какие узлы надо найти. По этой спеки собирается ES-запрос. */
trait AdnNodesSearchArgsT extends FtsAll with WithoutIds
with AnyOfPersonIdsDsa with DirectGeoParentsDsa with GeoParentsDsa with ShownTypeIdsDsa
with AdnRightsDsa with AdnSinksDsa with TestNodeDsa with NodeIsEnabledDsa with GeoDistanceDsa
with ShowInScNodeListDsa with WithIds with GeoDistanceSortDsa
with NameSort with Limit with Offset


/** Реализация интерфейса AdnNodesSearchArgsT с пустыми (дефолтовыми) значениями всех полей. */
trait AdnNodesSearchArgs extends AdnNodesSearchArgsT with FtsAllDflt with WithoutIdsDflt
with AnyOfPersonIdsDsaDflt with DirectGeoParentsDsaDflt
with GeoParentsDsaDflt with ShownTypeIdsDsaDflt with AdnRightsDsaDflt with AdnSinksDsaDflt with TestNodeDsaDflt
with NodeIsEnabledDsaDflt with GeoDistanceDsaDflt
with ShowInScNodeListDsaDflt with WithIdsDflt with GeoDistanceSortDsaDflt
with NameSortDflt with LimitDflt with OffsetDflt
{
  override def limit: Int = EsModelUtil.MAX_RESULTS_DFLT
  override def offset: Int = EsModelUtil.OFFSET_DFLT
}

class AdnNodesSearchArgsImpl
  extends AdnNodesSearchArgs


/** Враппер над аргументами поиска узлов, переданными в underlying. */
trait AdnNodesSearchArgsWrapper extends AdnNodesSearchArgsT with FtsAllWrap with WithoutIdsWrap
with AnyOfPersonIdsDsaWrapper
with DirectGeoParentsDsaWrapper with GeoParentsDsaWrapper with ShownTypeIdsDsaWrapper with AdnRightsDsaWrapper
with AdnSinksDsaWrapper with TestNodeDsaWrapper with NodeIsEnabledDsaWrapper with GeoDistanceDsaWrapper
with ShowInScNodeListDsaWrapper with WithIdsWrap
with GeoDistanceSortDsaWrapper with NameSortWrap
with LimitWrap with OffsetWrap
{
  override type WT <: AdnNodesSearchArgsT
}


/** Аддон для static-моделей (модели MAdnNode), добавляющий динамический поиск в статическую модель. */
trait AdnNodesSearch extends EsDynSearchStatic[AdnNodesSearchArgsT]

