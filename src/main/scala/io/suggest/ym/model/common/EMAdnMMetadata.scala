package io.suggest.ym.model.common

import org.joda.time.DateTime
import io.suggest.model.{EsModelT, EsModelStaticT}
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.JacksonWrapper
import io.suggest.model.EsModel._
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.util.SioEsUtil._, FieldIndexingVariants.FieldIndexingVariant

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

  val FLOOR_ESFN = "floor"
  val SECTION_ESFN = "section"

  val COLOR_ESFN = "color"
  val WELCOME_AD_ID = "welcomeAdId"
}

import EMAdnMMetadataStatic._


trait EMAdnMMetadataStatic[T <: EMAdnMMetadata[T]] extends EsModelStaticT[T] {

  private def fs(fn: String, iia: Boolean = true, index: FieldIndexingVariant = FieldIndexingVariants.no) = {
    FieldString(fn, include_in_all = iia, index = FieldIndexingVariants.no)
  }

  abstract override def generateMappingProps: List[DocField] = {
    FieldObject(METADATA_ESFN, enabled = true, properties = Seq(
      FieldDate(DATE_CREATED_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      // перемещено из legal
      fs(TOWN_ESFN, iia = true),
      fs(ADDRESS_ESFN, iia = true),
      fs(PHONE_ESFN, iia = true),
      fs(FLOOR_ESFN, iia = true, index = FieldIndexingVariants.not_analyzed),   // Внезапно, вдруг кто-то захочет найти все магазины на первом этаже.
      fs(SECTION_ESFN, iia = true),
      fs(SITE_URL_ESFN),
      // перемещено из visual
      FieldString(COLOR_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldString(WELCOME_AD_ID, index = FieldIndexingVariants.no, include_in_all = false)
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


/**
 *
 * @param name Отображаемое имя/название.
 * @param description Пользовательское описание.
 * @param town Город.
 * @param address Адрес в городе.
 * @param phone Телефонный номер.
 * @param floor Этаж.
 * @param section Номер секции/павильона/кабинета/помещения и т.д.
 * @param siteUrl Ссылка на сайт.@param dateCreated
 * @param color Предпочтительный цвет оформления.
 * @param welcomeAdId id карточки приветствия в [[io.suggest.ym.model.MWelcomeAd]].
 */
case class AdnMMetadata(
  var name          : String,
  var description   : Option[String] = None,
  dateCreated       : DateTime = DateTime.now,
  // перемещено из legal
  var town          : Option[String] = None,
  var address       : Option[String] = None,
  var phone         : Option[String] = None,
  var floor         : Option[String] = None,
  var section       : Option[String] = None,
  var siteUrl       : Option[String] = None,
  // перемещено из visual
  var color         : Option[String] = None,
  var welcomeAdId   : Option[String] = None
) {

  /** Загрузить строки из другого объекта метаданных. */
  @JsonIgnore
  def loadUserFieldsFrom(other: AdnMMetadata) {
    if (other != null) {
      name = other.name
      description = other.description
      // перемещено из legal
      town = other.town
      address = other.address
      phone = other.phone
      floor = other.floor
      section = other.section
      siteUrl = other.siteUrl
    }
  }

}

