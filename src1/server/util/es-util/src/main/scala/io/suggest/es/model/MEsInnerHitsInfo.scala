package io.suggest.es.model

import japgolly.univeq.UnivEq
import org.apache.commons.collections4.IteratorUtils
import org.elasticsearch.index.query.InnerHitBuilder
import org.elasticsearch.search.fetch.subphase.{FetchSourceContext, FieldAndFormat}

import scala.jdk.CollectionConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.04.2020 12:45
  * Description: Модель описания инфы по innerHits.
  */
object MEsInnerHitsInfo {

  @inline implicit def univEq: UnivEq[MEsInnerHitsInfo] = UnivEq.derive


  def buildInfoOpt(esIhInfoOpt: Option[MEsInnerHitsInfo], nameSuffix: Option[String] = None): Option[InnerHitBuilder] =
    esIhInfoOpt.map( buildInfo(_, nameSuffix) )

  def buildInfo(esIhInfo: MEsInnerHitsInfo, nameSuffix: Option[String] = None): InnerHitBuilder = {
    // Включён возврат предикатов, на основе которых отобраны карточки:
    val b = new InnerHitBuilder()
      .setFetchSourceContext( FetchSourceContext.DO_NOT_FETCH_SOURCE )
      .setDocValueFields(
        IteratorUtils.toList(
          esIhInfo
            .fields
            .iterator
            .map { fieldName =>
              new FieldAndFormat( fieldName, EsModelUtil.USE_FIELD_MAPPING )
            }
            .asJava
        )
      )

    var nameOpt = esIhInfo.name

    for (suffix <- nameSuffix)
      nameOpt = Some( nameOpt.fold(suffix)(_ + suffix) )

    nameOpt foreach b.setName

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
