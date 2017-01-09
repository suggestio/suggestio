package io.suggest.model.es

import org.elasticsearch.action.index.IndexRequestBuilder
import io.suggest.util.SioEsUtil.laFuture2sFuture

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 17:40
 * Description: Трейты для реализации модели, которая является child-моделью на стороне ES.
 */

trait EsChildModelStaticT extends EsModelCommonStaticT {

  override type T <: EsChildModelT

  import mCommonDi._

  def prepareGet(id: String, parentId: String) = prepareGetBase(id).setParent(parentId)
  def prepareUpdate(id: String, parentId: String) = prepareUpdateBase(id).setParent(parentId)
  def prepareDelete(id: String, parentId: String) = prepareDeleteBase(id).setParent(parentId)

  /**
   * Существует ли указанный магазин в хранилище?
   * @param id id элемента.
   * @param parentId id родительского элемента.
   * @return true/false
   */
  def isExist(id: String, parentId: String): Future[Boolean] = {
    prepareGet(id, parentId)
      .setFields()
      .setFetchSource(false)
      .execute()
      .map { _.isExists }
  }

  def get(id: String, parentId: String): Future[Option[T]] = {
    prepareGet(id, parentId)
      .execute()
      .map { v =>
        deserializeGetRespFull(v)
          .filter { _.parentId == parentId }
      }
  }


  /**
   * Выбрать документ из хранилища без парсинга. Вернуть сырое тело документа (его контент).
   * @param id id документа.
   * @return Строка json с содержимым документа или None.
   */
  def getRawContent(id: String, parentId: String): Future[Option[String]] = {
    prepareGet(id, parentId)
      .execute()
      .map { EsModelUtil.deserializeGetRespBodyRawStr }
  }

  /**
   * Прочитать документ как бы всырую.
   * @param id id документа.
   * @return Строка json с документом полностью или None.
   */
  def getRaw(id: String, parentId: String): Future[Option[String]] = {
    prepareGet(id, parentId)
      .execute()
      .map { EsModelUtil.deserializeGetRespFullRawStr }
  }

  /**
   * Удалить документ по id.
   * @param id id документа.
   * @param parentId id родительского документа, чтобы es мог вычислить шарду.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  def delete(id: String, parentId: String): Future[Boolean] = {
    prepareDelete(id, parentId)
      .execute()
      .map { _.isFound }
  }

  def resave(id: String, parentId: String): Future[Option[String]] = {
    resaveBase( get(id, parentId) )
  }


  def reget(inst0: T): Future[Option[T]] = {
    get(inst0.id.get, inst0.parentId)
  }

  override def prepareIndexNoVsn(m: T): IndexRequestBuilder = {
    super.prepareIndexNoVsn(m)
      .setParent(m.parentId)
  }

}


/** Динамическая сторона child-модели. */
trait EsChildModelT extends EsModelCommonT {

  def parentId: String

}
