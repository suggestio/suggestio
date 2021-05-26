package io.suggest.es.model

import io.suggest.es.util.SioEsUtil
import japgolly.univeq.UnivEq
import org.apache.commons.collections4.IteratorUtils
import org.elasticsearch.index.query.InnerHitBuilder
import org.elasticsearch.search.fetch.subphase.DocValueFieldsContext.FieldAndFormat
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
      .setDocValueFields(
        IteratorUtils.toList(
          esIhInfo
            .fields
            .iterator
            .map { fieldName =>
              new FieldAndFormat( fieldName, SioEsUtil.USE_FIELD_MAPPING )
            }
            .asJava
        )
      )

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
