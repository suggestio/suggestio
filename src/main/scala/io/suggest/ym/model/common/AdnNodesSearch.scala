package io.suggest.ym.model.common

import io.suggest.model.EsModel
import io.suggest.model.common._
import io.suggest.ym.model.ad._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.08.14 14:26
 * Description: Аддон для модели MAdnNode, которая отвечает за сборку и исполнение поисковых запросов узлов рекламной
 * сети.
 */

/** Интерфейс для описания критериев того, какие узлы надо найти. По этой спеки собирается ES-запрос. */
trait AdnNodesSearchArgsT extends TextQueryDsa with WithoutIdsDsa with CompanyIdsDsa with AdnSupIdsDsa
with AnyOfPersonIdsDsa with AdvDelegateAdnIdsDsa with DirectGeoParentsDsa with GeoParentsDsa with ShownTypeIdsDsa
with AdnRightsDsa with AdnSinksDsa with TestNodeDsa with NodeIsEnabledDsa with GeoDistanceDsa
with GeoIntersectsWithPreIndexedDsa with ShowInScNodeListDsa with WithIdsDsa with GeoDistanceSortDsa with LogoImgExistsDsa
with NameSortDsa with RoutingDsa


/** Реализация интерфейса AdnNodesSearchArgsT с пустыми (дефолтовыми) значениями всех полей. */
trait AdnNodesSearchArgs extends AdnNodesSearchArgsT with TextQueryDsaDflt with WithoutIdsDsaDflt with CompanyIdsDsaDflt
with AdnSupIdsDsaDflt with AnyOfPersonIdsDsaDflt with AdvDelegateAdnIdsDsaDflt with DirectGeoParentsDsaDflt
with GeoParentsDsaDflt with ShownTypeIdsDsaDflt with AdnRightsDsaDflt with AdnSinksDsaDflt with TestNodeDsaDflt
with NodeIsEnabledDsaDflt with GeoDistanceDsaDflt with GeoIntersectsWithPreIndexedDsaDftl
with ShowInScNodeListDsaDflt with WithIdsDsaDflt with GeoDistanceSortDsaDflt with LogoImgExistsDsaDflt
with NameSortDsaDflt with RoutingDsaDflt
{
  override def maxResults: Int = EsModel.MAX_RESULTS_DFLT
  override def offset: Int = EsModel.OFFSET_DFLT
}


/** Враппер над аргументами поиска узлов, переданными в underlying. */
trait AdnNodesSearchArgsWrapper extends AdnNodesSearchArgsT with TextQueryDsaWrapper with WithoutIdsDsaWrapper
with CompanyIdsDsaWrapper with AdnSupIdsDsaWrapper with AnyOfPersonIdsDsaWrapper with AdvDelegateAdnIdsDsaWrapper
with DirectGeoParentsDsaWrapper with GeoParentsDsaWrapper with ShownTypeIdsDsaWrapper with AdnRightsDsaWrapper
with AdnSinksDsaWrapper with TestNodeDsaWrapper with NodeIsEnabledDsaWrapper with GeoDistanceDsaWrapper
with GeoIntersectsWithPreIndexedDsaWrapper with ShowInScNodeListDsaWrapper with WithIdsDsaWrapper
with GeoDistanceSortDsaWrapper with LogoImgExistsDsaWrapper with NameSortDsaWrapper with RoutingDsaWrapper
{
  override type WT <: AdnNodesSearchArgsT
}


/** Аддон для static-моделей (модели MAdnNode), добавляющий динамический поиск в статическую модель. */
trait AdnNodesSearch extends EsDynSearchStatic[AdnNodesSearchArgsT]

