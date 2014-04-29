package io.suggest.ym.model.common

import org.joda.time.DateTime
import io.suggest.model.{EsModelT, EsModelStaticT}
import io.suggest.util.JacksonWrapper
import io.suggest.model.EsModel._
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.util.SioEsUtil._, FieldIndexingVariants.FieldIndexingVariant
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 13:43
 * Description: Метаданные участников рекламной сети. Всякие малоинтересные вещи складываются тут
 * (дата создания и т.д.).
 */

object EMAdnMMetadataStatic {
  /** Название поля с объектом метаданных. */
  val FLOOR_ESFN = "floor"
  val SECTION_ESFN = "section"

  val COLOR_ESFN = "color"
  val WELCOME_AD_ID = "welcomeAdId"

  def META_FLOOR_ESFN = META_ESFN + "." + FLOOR_ESFN
}

import EMAdnMMetadataStatic._


trait EMAdnMMetadataStatic extends EsModelStaticT {

  override type T <: EMAdnMMetadata

  private def fs(fn: String, iia: Boolean = true, index: FieldIndexingVariant = FieldIndexingVariants.no) = {
    FieldString(fn, include_in_all = iia, index = FieldIndexingVariants.no)
  }

  abstract override def generateMappingProps: List[DocField] = {
    FieldObject(META_ESFN, enabled = true, properties = Seq(
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
      case (META_ESFN, value) =>
        acc.meta = JacksonWrapper.convert[AdnMMetadata](value)
    }
  }

}

trait EMAdnMMetadata extends EsModelT {
  override type T <: EMAdnMMetadata

  var meta: AdnMMetadata

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    META_ESFN -> meta.toPlayJson :: super.writeJsonFields(acc)
  }

  /** Загрузка новых значений *пользовательских* полей из указанного экземпляра такого же класса.
    * Полезно при edit form sumbit после накатывания маппинга формы на реквест. */
  override def loadUserFieldsFrom(other: T) {
    super.loadUserFieldsFrom(other)
    meta.loadUserFieldsFrom(other.meta)
  }
}


/**
 * Экземпляр контейнера метаданных узла.
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

  /** Статически-типизированный json-генератор. */
  @JsonIgnore
  def toPlayJson: JsObject = {
    var acc0: FieldsJsonAcc = List(
      NAME_ESFN -> JsString(name),
      DATE_CREATED_ESFN -> date2JsStr(dateCreated)
    )
    if (description.isDefined)
      acc0 ::= DESCRIPTION_ESFN -> JsString(description.get)
    if (town.isDefined)
      acc0 ::= TOWN_ESFN -> JsString(town.get)
    if (address.isDefined)
      acc0 ::= ADDRESS_ESFN -> JsString(address.get)
    if (phone.isDefined)
      acc0 ::= PHONE_ESFN -> JsString(phone.get)
    if (floor.isDefined)
      acc0 ::= FLOOR_ESFN -> JsString(floor.get)
    if (section.isDefined)
      acc0 ::= SECTION_ESFN -> JsString(section.get)
    if (siteUrl.isDefined)
      acc0 ::= SITE_URL_ESFN -> JsString(siteUrl.get)
    if (color.isDefined)
      acc0 ::= COLOR_ESFN -> JsString(color.get)
    if (welcomeAdId.isDefined)
      acc0 ::= WELCOME_AD_ID -> JsString(welcomeAdId.get)
    JsObject(acc0)
  }

}

