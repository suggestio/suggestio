package io.suggest.ym.model.ad

import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.ym.model.common._

/** Интерфейс для передачи параметров поиска объявлений в индексе/типе. */
trait AdsSearchArgsT extends DynSearchArgs with TextQueryDsa with ReceiversDsa with ProducerIdsDsa with UserCatIdDsa
with GenerationSortDsa with WithoutIdsDsa with ReceiversDsaOnlyPublishedByDefault {

  override def generationSortingEnabled = qOpt.isEmpty

}


/** Дефолтовые значения аргументов поиска рекламных карточек. */
trait AdsSearchArgsDflt extends AdsSearchArgsT with DynSearchArgsDflt with TextQueryDsaDflt with ReceiversDsaDflt
with ProducerIdsDsaDflt with UserCatIdDsaDflt with GenerationSortDsaDflt with WithoutIdsDsaDflt



/** Враппер для аргументов поиска рекламных карточек. */
trait AdsSearchArgsWrapper extends AdsSearchArgsT with DynSearchArgsWrapper with TextQueryDsaWrapper
with ReceiversDsaWrapper with ProducerIdsDsaWrapper with UserCatIdDsaWrapper with GenerationSortDsaWrapper
with WithoutIdsDsaWrapper {
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
    // Необходимо выкинуть из запроса ненужные части.
    val adSearch2 = new AdsSearchArgsWrapper {
      override type WT = AdsSearchArgsT
      override def _dsArgsUnderlying = adSearch
      override def generationOpt = None
    }
    super.dynCount(adSearch2)
  }

}
