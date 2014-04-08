package io.suggest.ym.model.common

import org.joda.time.DateTime
import io.suggest.model.{EsModelT, EsModelStaticT}
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.JacksonWrapper
import io.suggest.util.SioEsUtil._
import io.suggest.model.EsModel._
import com.fasterxml.jackson.annotation.JsonIgnore

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


trait EMAdnMMetadataStatic[T <: EMAdnMMetadata[T]] extends EsModelStaticT[T] {

  abstract override def generateMappingProps: List[DocField] = {
    FieldObject(METADATA_ESFN, enabled = true, properties = Seq(
      FieldDate(DATE_CREATED_ESFN, index = FieldIndexingVariants.no, include_in_all = false)
    )) :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (METADATA_ESFN, value) =>
        acc.meta = JacksonWrapper.convert[AdnMMetadata](value)
    }
  }

}

trait EMAdnMMetadata[T <: EMAdnMMetadata[T]] extends EsModelT[T] {

  var meta: AdnMMetadata

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    val mdSer = JacksonWrapper.serialize(meta)
    acc.rawField(METADATA_ESFN, mdSer.getBytes)
  }

  /** Загрузка новых значений *пользовательских* полей из указанного экземпляра такого же класса.
    * Полезно при edit form sumbit после накатывания маппинга формы на реквест. */
  override def loadUserFieldsFrom(other: T) {
    super.loadUserFieldsFrom(other)
    meta.loadUserFieldsFrom(other.meta)
  }
}



case class AdnMMetadata(
  var name: String,
  var description: Option[String] = None,
  dateCreated: DateTime = DateTime.now
) {

  /** Загрузить строки из другого объекта метаданных. */
  @JsonIgnore
  def loadUserFieldsFrom(other: AdnMMetadata) {
    if (other != null) {
      name = other.name
      description = other.description
    }
  }

}

