package io.suggest.ym.model.common

import io.suggest.model._
import org.elasticsearch.common.xcontent.XContentBuilder
import EsModel._
import io.suggest.util.SioEsUtil._
import scala.collection.JavaConversions._
import FieldIndexingVariants.FieldIndexingVariant

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.14 18:44
 * Description: Поддержка полей-свойств юр.лиц.
 */

object EMLegalEntity {
  val LEGAL_INFO_ESFN = "legalInfo"

  val FLOOR_ESFN = "floor"
  val SECTION_ESFN = "section"
}

import EMLegalEntity._


trait EMLegalEntityStatic[T <: EMLegalEntity[T]] extends EsModelStaticT[T] {
  abstract override def generateMappingProps: List[DocField] = {
    FieldObject(LEGAL_INFO_ESFN, enabled = true, properties = Seq(
      fs(TOWN_ESFN, iia = true),
      fs(ADDRESS_ESFN, iia = true),
      fs(PHONE_ESFN, iia = true),
      fs(FLOOR_ESFN, iia = true, index = FieldIndexingVariants.not_analyzed),   // Внезапно, вдруг кто-то захочет найти все магазины на первом этаже.
      fs(SECTION_ESFN, iia = true),
      fs(SITE_URL_ESFN)
    )) :: super.generateMappingProps
  }

  private def fs(fn: String, iia: Boolean = true, index: FieldIndexingVariant = FieldIndexingVariants.no) = {
    FieldString(fn, include_in_all = iia, index = FieldIndexingVariants.no)
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (LEGAL_INFO_ESFN, value: java.util.Map[_, _]) =>
        import acc.legalInfo
        if (legalInfo == null)
          acc.legalInfo = new AdnLegalEntityInfo
        value foreach {
          case (TOWN_ESFN, town)        => legalInfo.town = Option(stringParser(town))
          case (ADDRESS_ESFN, address)  => legalInfo.address = Option(addressParser(address))
          case (PHONE_ESFN, phone)      => legalInfo.phone = Option(stringParser(phone))
          case (FLOOR_ESFN, floor)      => legalInfo.floor = Option(stringParser(floor))
          case (SECTION_ESFN, section)  => legalInfo.section = Option(stringParser(section))
          case (SITE_URL_ESFN, siteUrl) => legalInfo.siteUrl = Option(stringParser(siteUrl))
        }
    }
  }
}

trait EMLegalEntity[T <: EMLegalEntity[T]] extends EsModelT[T] {

  var legalInfo: AdnLegalEntityInfo

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    // Быстро записать объект в JSON
    if (!legalInfo.isEmpty) {
      acc.startObject(LEGAL_INFO_ESFN)
      legalInfo.writeFields(acc)
      acc.endObject()
    }
  }

  /** Загрузка новых значений *пользовательских* полей из указанного экземпляра такого же класса.
    * Полезно при edit form sumbit после накатывания маппинга формы на реквест. */
  override def loadUserFieldsFrom(other: T): Unit = {
    super.loadUserFieldsFrom(other)
    legalInfo.updateFrom(other.legalInfo)
  }
}


/**
 * Информация по юр.лицу.
 * @param town Город.
 * @param address Адрес в городе.
 * @param phone Телефонный номер.
 * @param floor Этаж.
 * @param section Номер секции/павильона/кабинета/помещения и т.д.
 * @param siteUrl Ссылка на сайт.
 */
case class AdnLegalEntityInfo(
  var town          : Option[String] = None,
  var address       : Option[String] = None,
  var phone         : Option[String] = None,
  var floor         : Option[String] = None,
  var section       : Option[String] = None,
  var siteUrl       : Option[String] = None
) {
  /** Содержит ли этот case class какую-либо полезную нагрузку? */
  def isEmpty: Boolean = {
    productIterator.forall {
      case opt: Option[_]     => opt.isEmpty
      case l: Traversable[_]  => l.isEmpty
      case _                  => false
    }
  }

  /** Отрендерить все поля в json. */
  def writeFields(acc: XContentBuilder) {
    if (town.isDefined)
      acc.field(TOWN_ESFN, town.get)
    if (address.isDefined)
      acc.field(ADDRESS_ESFN, address.get)
    if (phone.isDefined)
      acc.field(PHONE_ESFN, phone.get)
    if (floor.isDefined)
      acc.field(FLOOR_ESFN, floor.get)
    if (section.isDefined)
      acc.field(SECTION_ESFN, section.get)
    if (siteUrl.isDefined)
      acc.field(SITE_URL_ESFN, siteUrl.get)
  }

  /** Загрузить новые значения полей из другого экземпляра [[AdnLegalEntityInfo]]. */
  def updateFrom(other: AdnLegalEntityInfo) {
    town = other.town
    address = other.address
    phone = other.phone
    floor = other.floor
    section = other.section
    siteUrl = other.siteUrl
  }
}

