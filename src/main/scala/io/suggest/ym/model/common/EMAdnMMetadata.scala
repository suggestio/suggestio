package io.suggest.ym.model.common

import org.joda.time.DateTime
import io.suggest.model.{EsModelT, EsModelStaticT}
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.JacksonWrapper
import io.suggest.util.SioEsUtil._
import io.suggest.model.EsModel._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 13:43
 * Description: Метаданные участников рекламной сети. Всякие малоинтересные вещи складываются тут
 * (дата создания и т.д.).
 */

object EMAdnMMetadataStatic {
  /** Название поля с объектом метаданных. */
  val METADATA_ESFN = "md"
}

import EMAdnMMetadataStatic._


trait EMAdnMMetadataStatic[T <: EMAdnMMetadata] extends EsModelStaticT[T] {

  abstract override def generateMappingProps: List[DocField] = {
    FieldObject(METADATA_ESFN, enabled = true, properties = Seq(
      FieldDate(DATE_CREATED_ESFN, index = FieldIndexingVariants.no, include_in_all = false)
    )) :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (METADATA_ESFN, value) =>
        acc.metadata = JacksonWrapper.convert[AdnMMetadata](value)
    }
  }

}

trait EMAdnMMetadata[T <: EMAdnMMetadata[T]] extends EsModelT[T] {

  var metadata: AdnMMetadata

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    val mdSer = JacksonWrapper.serialize(metadata)
    acc.rawField(METADATA_ESFN, mdSer.getBytes)
  }

  /** Загрузка новых значений *пользовательских* полей из указанного экземпляра такого же класса.
    * Полезно при edit form sumbit после накатывания маппинга формы на реквест. */
  override def loadUserFieldsFrom(other: T) {
    super.loadUserFieldsFrom(other)
    metadata.name = other.metadata.name
    metadata.description = other.metadata.description
  }
}



case class AdnMMetadata(
  var name: String,
  var description: Option[String] = None,
  dateCreated: DateTime = DateTime.now
)

