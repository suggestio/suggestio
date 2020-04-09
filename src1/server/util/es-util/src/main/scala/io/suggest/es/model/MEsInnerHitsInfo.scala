package io.suggest.es.model

import japgolly.univeq.UnivEq
import org.elasticsearch.index.query.InnerHitBuilder
import org.elasticsearch.search.fetch.subphase.FetchSourceContext
import scala.jdk.CollectionConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.04.2020 12:45
  * Description: Модель описания инфы по innerHits.
  */
object MEsInnerHitsInfo {

  @inline implicit def univEq: UnivEq[MEsInnerHitsInfo] = UnivEq.derive


  def buildInfoOpt(esIhInfoOpt: Option[MEsInnerHitsInfo]): Option[InnerHitBuilder] =
    esIhInfoOpt.map(buildInfo)
  def buildInfo(esIhInfo: MEsInnerHitsInfo): InnerHitBuilder = {
    // Включён возврат предикатов, на основе которых отобраны карточки:
    val b = new InnerHitBuilder()
      .setFetchSourceContext( FetchSourceContext.DO_NOT_FETCH_SOURCE )
      .setDocValueFields( esIhInfo.fields.asJava )

    esIhInfo.name foreach b.setName

    b
  }

}


/** Контейнер данных, описывающий inner-hits.
  *
  * @param fields Возвращать в ответе в inner_hits указанные поля с поддержкой doc_values.
  * @param name Название запроса. По умолчанию - имя nested-поля.
  */
final case class MEsInnerHitsInfo(
                                   fields   : Seq[String],
                                   name     : Option[String]    = None,
                                 )
