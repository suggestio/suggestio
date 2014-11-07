package models.im

import io.suggest.event.SioNotifierStaticClientI
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.client.Client
import org.elasticsearch.search.sort.{SortOrder, FieldSortBuilder, SortBuilder}
import util.PlayMacroLogsImpl
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model.{EsModel, EsModelPlayJsonT, EsModelT, EsModelStaticT}
import io.suggest.util.SioEsUtil._
import org.joda.time.DateTime
import play.api.libs.json._

import scala.collection.Map
import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.11.14 17:58
 * Description: Именованные галереи картинок. Изначально были доступны только для суперюзеров.
 */
object MGallery extends EsModelStaticT with PlayMacroLogsImpl {

  override type T = MGallery

  override val ES_TYPE_NAME = "ig"

  val NAME_ESFN           = "n"
  val IMGS_ESFN           = "i"
  val DESCR_ESFN          = "d"
  val MODIFIED_BY_ESFN    = "mb"
  val DATE_CREATED_ESFN   = "dc"
  val DATE_MODIFIED_ESFN  = "dm"

  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldId(path = NAME_ESFN),
      FieldAll(enabled = true),
      FieldSource(enabled = true)
    )
  }

  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(NAME_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(IMGS_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = false),
      FieldString(DESCR_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(MODIFIED_BY_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldDate(DATE_CREATED_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldDate(DATE_MODIFIED_ESFN, index = FieldIndexingVariants.no, include_in_all = false)
    )
  }

  /**
   * Десериализация одного элементам модели.
   * @param id id документа.
   * @param m Карта, распарсенное json-тело документа.
   * @return Экземпляр модели.
   */
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    MGallery(
      name = m.get(NAME_ESFN)
        .map(EsModel.stringParser)
        .orElse(id)
        .get,
      imgs = m.get(IMGS_ESFN)
        .fold(List.empty[MImg]) { raw =>
          EsModel.iteratorParser(raw)
            .map(EsModel.stringParser)
            .map(MImg.apply)
            .toList
        },
      descr = m.get(DESCR_ESFN)
        .map(EsModel.stringParser),
      dateCreated = m.get(DATE_CREATED_ESFN)
        .fold(DateTime.now)(EsModel.dateTimeParser),
      modifiedBy = m.get(MODIFIED_BY_ESFN)
        .map(EsModel.stringParser),
      dateModified = m.get(DATE_MODIFIED_ESFN)
        .map(EsModel.dateTimeParser),
      versionOpt = version
    )
  }


  /** Выхлоп getAll() должен быть отсортированным. */
  override def getAllReq(maxResults: Int, offset: Int, withVsn: Boolean)(implicit client: Client): SearchRequestBuilder = {
    super.getAllReq(maxResults, offset, withVsn)
      .addSort(NAME_ESFN, SortOrder.ASC)
  }



}


import MGallery._


case class MGallery(
  name            : String,
  imgs            : List[MImg],
  descr           : Option[String] = None,
  dateCreated     : DateTime = DateTime.now(),
  modifiedBy      : Option[String] = None,
  dateModified    : Option[DateTime] = None,
  versionOpt      : Option[Long] = None
) extends EsModelPlayJsonT with EsModelT {

  @JsonIgnore
  override def id: Option[String] = Some(name)

  @JsonIgnore
  override type T = MGallery

  @JsonIgnore
  override def companion = MGallery

  override def writeJsonFields(acc0: FieldsJsonAcc): FieldsJsonAcc = {
    var acc =
      NAME_ESFN -> JsString(name) ::
      DATE_CREATED_ESFN -> EsModel.date2JsStr(dateCreated) ::
      acc0
    if (imgs.nonEmpty)
      acc ::= IMGS_ESFN -> JsArray( imgs.map(img => JsString(img.fileName)) )
    if (descr.nonEmpty)
      acc ::= DESCR_ESFN -> JsString(descr.get)
    if (modifiedBy.nonEmpty)
      acc ::= MODIFIED_BY_ESFN -> JsString(modifiedBy.get)
    if (dateModified.nonEmpty)
      acc ::= DATE_MODIFIED_ESFN -> EsModel.date2JsStr(dateModified.get)
    acc
  }

  /** Стирание ресурсов, относящихся к этой модели. Т.е. картинок, на которые ссылкается эта модель. */
  override def eraseResources(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    val eraseImgsFut = Future.traverse(imgs) { img =>
      MImg.deleteAllFor(img.rowKey)
    }
    super.eraseResources
      .flatMap { _ => eraseImgsFut }
  }

  /** Удалить текущий ряд из таблицы. Если ключ не выставлен, то сразу будет экзепшен.
    * @return true - всё ок, false - документ не найден.
    */
  override def delete(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    eraseResources flatMap { _ =>
      super.delete
    }
  }

}
