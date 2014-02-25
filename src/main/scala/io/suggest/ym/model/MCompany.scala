package io.suggest.ym.model

import org.joda.time.DateTime
import scala.collection.Map
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.SioConstants._
import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.client.Client
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model._
import io.suggest.model.EsModel._
import io.suggest.util.SioEsUtil._

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

  override def generateMapping: XContentBuilder = jsonGenerator { implicit b =>
    new IndexMapping(
      typ = ES_TYPE_NAME,
      static_fields = Seq(
        FieldSource(enabled = true),
        FieldAll(enabled = false, analyzer = FTS_RU_AN)
      ),
      properties = Seq(
        FieldString(
          id = NAME_ESFN,
          include_in_all = true,
          index = FieldIndexingVariants.no
        ),
        FieldDate(
          id = DATE_CREATED_ESFN,
          include_in_all = false,
          index = FieldIndexingVariants.no
        )
      )
    )
  }

  override def applyMap(m: Map[String, AnyRef], acc: MCompany): MCompany = {
    m foreach {
      case (NAME_ESFN, value) =>
        acc.name = nameParser(value)
      case (DATE_CREATED_ESFN, value) =>
        acc.date_created = dateCreatedParser(value)
    }
    acc
  }

  override protected def dummy(id: String) = MCompany(id = Some(id), name = null)

  /**
   * Удалить документ по id.
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  override def deleteById(id: String)(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    val martsCountFut = MMart.countByCompanyId(id)
    val fkFut = for {
      shopsCount <- MShop.countByCompanyId(id)
      martsCount <- martsCountFut
    } yield {
      martsCount -> shopsCount
    }
    fkFut flatMap {
      case (0L, 0L) =>
        super.deleteById(id)

      case (martsCount, shopsCount) =>
        Future failed new ForeignKeyException(s"Cannot delete company with $martsCount marts and $shopsCount shops.")
    }
  }
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
  def company(implicit ec:ExecutionContext, client: Client) = getById(company_id)
}
