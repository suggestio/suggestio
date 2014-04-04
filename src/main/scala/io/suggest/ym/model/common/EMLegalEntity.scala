package io.suggest.ym.model.common

import io.suggest.model._
import org.elasticsearch.common.xcontent.XContentBuilder
import EsModel._
import io.suggest.util.SioEsUtil._
import io.suggest.util.JacksonWrapper

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.14 18:44
 * Description: Поддержка полей-свойств юр.лиц.
 */

object EMLegalEntity {
  val LEGAL_INFO_ESFN = "legalInfo"
}

import EMLegalEntity._

trait EMLegalEntityStatic[T <: EMLegalEntity[T]] extends EsModelStaticT[T] {
  abstract override def generateMappingProps: List[DocField] = {
    FieldObject(LEGAL_INFO_ESFN, enabled = true, properties = Seq(
      fs(TOWN_ESFN),
      fs(ADDRESS_ESFN),
      fs(PHONE_ESFN),
      fs(SITE_URL_ESFN)
    )) :: super.generateMappingProps
  }

  private def fs(fn: String, iia: Boolean = true) = FieldString(fn, include_in_all = iia, index = FieldIndexingVariants.no)

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (LEGAL_INFO_ESFN, value) => JacksonWrapper.convert[AdnLegalEntityInfo](value)
    }
  }
}

trait EMLegalEntity[T <: EMLegalEntity[T]] extends EsModelT[T] {

  var legalInfo: AdnLegalEntityInfo

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    val leInfoSer = JacksonWrapper.serialize(legalInfo)
    acc.rawField(LEGAL_INFO_ESFN, leInfoSer.getBytes)
  }
}


/**
 * Информация по юр.лицу.
 * @param town Город.
 * @param address Адрес в городе.
 * @param phone Телефонный номер.
 * @param siteUrl Ссылка на сайт.
 */
case class AdnLegalEntityInfo(
  var town          : Option[String] = None,
  var address       : Option[String] = None,
  var phone         : Option[String] = None,
  var siteUrl       : Option[String] = None
)
