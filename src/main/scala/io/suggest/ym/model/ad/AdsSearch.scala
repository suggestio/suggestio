package io.suggest.ym.model.ad


import io.suggest.model.search._

import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.ym.model.common._


/** Интерфейс для передачи параметров поиска объявлений в индексе/типе. */
trait AdsSearchArgsT extends DynSearchArgs with TextQueryDsa with ReceiversDsa with ProducerIdsDsa with UserCatIdDsa
with GenerationSortDsa with WithoutIdsDsa with ReceiversDsaOnlyPublishedByDefault with Limit with Offset {

  override def generationSortingEnabled = qOpt.isEmpty
}


/** Дефолтовые значения аргументов поиска рекламных карточек. */
trait AdsSearchArgsDflt extends AdsSearchArgsT with TextQueryDsaDflt with ReceiversDsaDflt
with ProducerIdsDsaDflt with UserCatIdDsaDflt with GenerationSortDsaDflt with WithoutIdsDsaDflt
with LimitDflt with OffsetDflt



/** Враппер для аргументов поиска рекламных карточек. */
trait AdsSearchArgsWrapper extends AdsSearchArgsT with DynSearchArgsWrapper with TextQueryDsaWrapper
with ReceiversDsaWrapper with ProducerIdsDsaWrapper with UserCatIdDsaWrapper with GenerationSortDsaWrapper
with WithoutIdsDsaWrapper with LimitWrap with OffsetWrap {
  override type WT <: AdsSearchArgsT
}


/** Если нужно добавить в рекламную модель поиск по рекламным карточкам, то следует задействовать вот этот трейт. */
trait AdsSimpleSearchT extends EsDynSearchStatic[AdsSearchArgsT] {

  /**
   * Посчитать кол-во рекламных карточек, подходящих под запрос.
   * @param adSearch Экземпляр, описывающий поисковый запрос.
   * @return Фьючерс с кол-вом совпадений.
   */
  override def dynCount(adSearch: AdsSearchArgsT)(implicit ec: ExecutionContext, client: Client): Future[Long] = {

    // Если в search-аргументах есть определённо ненужные составляющие, то надо их срезать.
    val adSearch2: AdsSearchArgsT = {
      if (adSearch.generationOpt.nonEmpty) {
        // Необходимо выкинуть из запроса ненужные части.
        new AdsSearchArgsWrapper {
          override type WT = AdsSearchArgsT
          override def _dsArgsUnderlying = adSearch
          override def generationOpt = None
        }
      } else {

        adSearch
      }
    }

    super.dynCount(adSearch2)
  }

}
