package io.suggest.ym.model.common

import io.suggest.model._
import org.elasticsearch.common.xcontent.XContentBuilder
import EsModel._
import io.suggest.util.SioEsUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.14 18:44
 * Description: Поддержка полей-свойств юр.лиц.
 */

trait EMLegalEntityStatic[T <: EMLegalEntity[T]] extends EsModelStaticT[T] {
  abstract override def generateMappingProps: List[DocField] = {
    FieldString(TOWN_ESFN, include_in_all = true, index = FieldIndexingVariants.no) ::
    FieldString(ADDRESS_ESFN, include_in_all = true, index = FieldIndexingVariants.no) ::
    FieldString(PHONE_ESFN, include_in_all = false, index = FieldIndexingVariants.no) ::
    FieldString(SITE_URL_ESFN, include_in_all = false, index = FieldIndexingVariants.no) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (ADDRESS_ESFN, value)        => acc.addressOpt = Option(addressParser(value))
      case (TOWN_ESFN, value)           => acc.townOpt    = Option(stringParser(value))
      case (PHONE_ESFN, value)          => acc.phoneOpt   = Option(stringParser(value))
      case (SITE_URL_ESFN, value)       => acc.siteUrl    = Option(siteUrlParser(value))
    }
  }
}

trait EMLegalEntity[T <: EMLegalEntity[T]] extends EsModelT[T] {

  var townOpt          : Option[String]
  var addressOpt       : Option[String]
  var phoneOpt         : Option[String]
  var siteUrl          : Option[String]

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    if (townOpt.isDefined)
      acc.field(TOWN_ESFN, townOpt.get)
    if(addressOpt.isDefined)
      acc.field(ADDRESS_ESFN, addressOpt.get)
    if (phoneOpt.isDefined)
      acc.field(PHONE_ESFN, phoneOpt.get)
  }
}
