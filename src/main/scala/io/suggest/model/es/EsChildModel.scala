package io.suggest.model.es

import io.suggest.event.SioNotifierStaticClientI
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.action.update.UpdateRequestBuilder
import org.elasticsearch.client.Client
import io.suggest.util.SioEsUtil.laFuture2sFuture

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 17:40
 * Description: Трейты для реализации модели, которая является child-моделью на стороне ES.
 */

trait EsChildModelStaticT extends EsModelCommonStaticT {

  override type T <: EsChildModelT

  def prepareGet(id: String, parentId: String)(implicit client: Client) = prepareGetBase(id).setParent(parentId)
  def prepareTermVector(id: String, parentId: String)(implicit client: Client) = prepareTermVectorBase(id).setParent(parentId)
  def prepareUpdate(id: String, parentId: String)(implicit client: Client) = prepareUpdateBase(id).setParent(parentId)
  def prepareDelete(id: String, parentId: String)(implicit client: Client) = prepareDeleteBase(id).setParent(parentId)

  /**
   * Существует ли указанный магазин в хранилище?
   * @param id id элемента.
   * @param parentId id родительского элемента.
   * @return true/false
   */
  def isExist(id: String, parentId: String)(implicit ec:ExecutionContext, client: Client): Future[Boolean] = {
    prepareGet(id, parentId)
      .setFields()
      .setFetchSource(false)
      .execute()
      .map { _.isExists }
  }

  def get(id: String, parentId: String)(implicit ec:ExecutionContext, client: Client): Future[Option[T]] = {
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
  def getRawContent(id: String, parentId: String)(implicit ec:ExecutionContext, client: Client): Future[Option[String]] = {
    prepareGet(id, parentId)
      .execute()
      .map { EsModelUtil.deserializeGetRespBodyRawStr }
  }

  /**
   * Прочитать документ как бы всырую.
   * @param id id документа.
   * @return Строка json с документом полностью или None.
   */
  def getRaw(id: String, parentId: String)(implicit ec:ExecutionContext, client: Client): Future[Option[String]] = {
    prepareGet(id, parentId)
      .execute()
      .map { EsModelUtil.deserializeGetRespFullRawStr }
  }

  /**
   * Удалить документ по id.
   * @param id id документа.
   * @param parentId id родительского документа, чтобы es мог вычислить шарду.
   * @param ignoreResources Не пытаться удалять ресурсы модели. true, если ресурсы уже удалены.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  def delete(id: String, parentId: String, ignoreResources: Boolean = false)
            (implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    val delResFut = maybeEraseResources(ignoreResources, get(id, parentId))
    delResFut flatMap { _ =>
      prepareDelete(id, parentId)
        .execute()
        .map { _.isFound }
    }
  }

  def resave(id: String, parentId: String)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Option[String]] = {
    EsModelUtil.resaveBase( get(id, parentId) )
  }

  def reget(inst0: T)(implicit ec: ExecutionContext, client: Client): Future[Option[T]] = {
    get(inst0.id.get, inst0.parentId)
  }

}


/** Динамическая сторона child-модели. */
trait EsChildModelT extends EsModelCommonT {

  override type T <: EsChildModelT

  def parentId: String
  override def companion: EsChildModelStaticT

  override def prepareUpdate(implicit client: Client): UpdateRequestBuilder = {
    super.prepareUpdate.setParent(parentId)
  }

  override def indexRequestBuilder(implicit client: Client): IndexRequestBuilder = {
    super.indexRequestBuilder.setParent(parentId)
  }

  override def companionDelete(_id: String, ignoreResources: Boolean)
                              (implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    companion.delete(_id, parentId, ignoreResources)
  }

  override def prepareDelete(implicit client: Client) = companion.prepareDelete(id.get, parentId)
}
