package models

import org.joda.time.DateTime
import EsModel._
import scala.collection.Map
import org.elasticsearch.common.xcontent.XContentBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.01.14 17:40
 * Description: Компания - верхний элемент иерархии структуры юр.лиц.
 * Компания может владеть магазинами и торговыми центрами.
 */

object MCompany extends EsModelStaticT[MCompany] {

  type CompanyId_t = String

  override val ES_TYPE_NAME: String = "company"

  override def applyMap(m: Map[String, AnyRef], acc: MCompany): MCompany = {
    m foreach {
      case (NAME_ESFN, value) =>
        acc.name = nameParser(value)
      case (DATE_CREATED_ESFN, value) =>
        acc.date_created = dateCreatedParser(value)
    }
    acc
  }

  override protected def dummy(id: String) = MCompany(name = null)
}


import MCompany._

/**
 * Экземпляр распарсенного ряда БД.
 * @param name Название конторы.
 * @param id id по базе.
 * @param date_created Дата создания ряда/компании.
 */
case class MCompany(
  var name          : String,
  id                : Option[MCompany.CompanyId_t] = None,
  var date_created  : DateTime = null
) extends EsModelT[MCompany] with CompanyShopsSel with CompanyMartsSel {
  def company_id = id.get
  def companion = MCompany

  override def writeJsonFields(acc: XContentBuilder) {
    acc.field(NAME_ESFN, name)
    if (date_created == null)
      date_created = DateTime.now()
    acc.field(DATE_CREATED_ESFN, date_created)
  }
}


trait MCompanySel {
  def company_id: CompanyId_t
  def company = getById(company_id)
}
