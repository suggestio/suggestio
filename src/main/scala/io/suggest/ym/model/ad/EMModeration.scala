package io.suggest.ym.model.ad

import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model._
import io.suggest.util.SioEsUtil._
import io.suggest.ym.model.common.{EMReceivers, EMProducerId}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.{QueryBuilder, FilterBuilders, QueryBuilders}
import org.joda.time.DateTime
import play.api.libs.json._
import java.{util => ju}
import scala.collection.JavaConversions._
import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.14 9:50
 * Description: Поле с инфой по модерации рекламной карточки.
 * Изначально было лишь одно поле: отметка о возможности бесплатного размещения.
 */
object EMModeration {

  /** Название поля, с которого начинается всё остальное в этом файле.
    * В нём хранится объект с данными по модерации. */
  val MODERATION_ESFN = "mdr"

  /** MVEL-код определения рекламной карточки, у которой producer_id совпадает с одним из ресиверов.
    * Сначала скрипт проверяет наличие былой проверки и отфильтровывает, если уже выверено.
    * Затем ищем producerId в списке ресиверов, которые хоть что-то опубликовали. */
  val FREE_ADV_NEED_MDR_MVEL = {
    import EMProducerId.PRODUCER_ID_ESFN
    import EMReceivers._
    import ModerationInfo.FREE_ADV_ESFN, FreeAdvStatus.IS_ALLOWED_ESFN
    s"""
      |if (doc['$MODERATION_ESFN.$FREE_ADV_ESFN.$IS_ALLOWED_ESFN'].values != empty)
      |  return false;
      |prodId = doc['$PRODUCER_ID_ESFN'].value;
      |rcvrs = _source['$RECEIVERS_ESFN'];
      |if (rcvrs != null) {
      |  foreach (rcvr: rcvrs) {
      |    if (rcvr['$RECEIVER_ID_ESFN'] == prodId && rcvr['$SLS_ESFN'] != empty)
      |      return true;
      |  }
      |}
      |return false;
    """.stripMargin
  }

  /** Добавить в query фильтрацию скриптом. */
  def queryFilteredFreeAdvNeedMdr(q0: QueryBuilder): QueryBuilder = {
    val filterSelfAdv = FilterBuilders.scriptFilter(FREE_ADV_NEED_MDR_MVEL)
    QueryBuilders.filteredQuery(q0, filterSelfAdv)
  }

  /** Скомпилить аргументы поиска модерированных карточек в query. */
  def mdrSearchArgs2query(args: MdrSearchArgsI): QueryBuilder = {
    args.producerId.map[QueryBuilder] { producerId =>
      EMProducerId.producerIdQuery(producerId)
    } map { pqb =>
      args.freeAdvIsAllowed.fold {
        // Не задан искомый вердикт модерации. Найти саморазмещённые, но ещё не отмодерированные карточки:
        queryFilteredFreeAdvNeedMdr(pqb)
      } { freeAdvIsAllowed =>
        // Есть искомый вердикт модерации. Нужно фильтровать по полю.
        val fn = FreeAdvStatus.MDR_IS_ALLOWED_ESFN
        val f = FilterBuilders.termFilter(fn, freeAdvIsAllowed)
        QueryBuilders.filteredQuery(pqb, f)
      }
    } orElse {
      // producerId не задан. Нужно попытаться поискать по следующему параметру
      args.freeAdvIsAllowed.map { freeAdvIsAllowed =>
        val fn = FreeAdvStatus.MDR_IS_ALLOWED_ESFN
        QueryBuilders.termQuery(fn, freeAdvIsAllowed)
      }
    } getOrElse {
      // Не задано ничего для поиска по индексам. Фильтруем скриптом весь индекс+тип целиком.
      queryFilteredFreeAdvNeedMdr( QueryBuilders.matchAllQuery() )
    }
  }

}

import EMModeration._

/** Аддон для статического фунционала модели. */
trait EMModerationStatic extends EsModelStaticMutAkvT {
  override type T <: EMModerationMut

  abstract override def generateMappingProps: List[DocField] = {
    val mapping = FieldObject(MODERATION_ESFN, enabled = true, properties = ModerationInfo.generateMappingProps)
    mapping :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (MODERATION_ESFN, jmap) =>
        acc.moderation = ModerationInfo.deserialize(jmap)
    }
  }

  /** Собрать саморазмещённые карточки, у которых не было модерации вообще. */
  def findSelfAdvNonMdr(args: MdrSearchArgsI)(implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    prepareSearch
      .setQuery( mdrSearchArgs2query(args) )
      .setSize(args.maxResults)
      .setFrom(args.offset)
      .execute()
      .map { searchResp2list }
  }
}



/** Интерфейс к полю с инфой по модерации. */
trait EMModerationI {
  def moderation: ModerationInfo
}

/** Аддон с функционалом для сериализации поля. */
trait EMModeration extends EsModelPlayJsonT with EMModerationI {
  override type T <: EMModeration

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc1 = super.writeJsonFields(acc)
    if (moderation.nonEmpty)
      MODERATION_ESFN -> moderation.toPlayJson :: acc1
    else
      acc1
  }

  /** Сохранить значение поля модерации с помощью update, т.е. с контролем версий. */
  def saveModeration(implicit ec: ExecutionContext, client: Client): Future[_] = {
    val json = JsObject(Seq(
      MODERATION_ESFN -> moderation.toPlayJson
    ))
    prepareUpdate
      .setDoc(json.toString())
      .execute()
  }
}

/** mutable-версия поля. */
trait EMModerationMut extends EMModeration {
  var moderation: ModerationInfo
}



object ModerationInfo {
  val FREE_ADV_ESFN   = "fa"

  def generateMappingProps: List[DocField] = List(
    FieldObject(FREE_ADV_ESFN, enabled = true, properties = FreeAdvStatus.generateMappingProps)
  )

  val deserialize: PartialFunction[Any, ModerationInfo] = {
    case jmap: ju.Map[_,_] =>
      ModerationInfo(
        freeAdv = Option(jmap.get(FREE_ADV_ESFN)).map(FreeAdvStatus.deserialize)
      )
  }
}

/**
 * Инфа по модерации.
 * @param freeAdv Данные по возможности бесплатного размещения карточки (у себя самого, например).
 */
case class ModerationInfo(
  freeAdv: Option[FreeAdvStatus] = None
) {
  import ModerationInfo._

  def nonEmpty: Boolean = {
    productIterator.exists {
      case opt: Option[_] => opt.nonEmpty
      case _ => true
    }
  }
  def isEmpty = !nonEmpty

  def toPlayJson: JsObject = {
    var acc: FieldsJsonAcc = Nil
    if (freeAdv.nonEmpty)
      acc ::= FREE_ADV_ESFN -> freeAdv.get.toPlayJson
    JsObject(acc)
  }
}



object FreeAdvStatus {
  val IS_ALLOWED_ESFN = "ia"
  val WHEN_ESFN = "w"
  val BY_USER_ESFN = "bu"
  val REASON_ESFN = "r"

  def MDR_IS_ALLOWED_ESFN = MODERATION_ESFN + "." + ModerationInfo.FREE_ADV_ESFN + "." + IS_ALLOWED_ESFN

  /** Создать под-маппинг для индекса. */
  def generateMappingProps: List[DocField] = List(
    FieldBoolean(IS_ALLOWED_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldDate(WHEN_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
    FieldString(BY_USER_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldString(REASON_ESFN, index = FieldIndexingVariants.no, include_in_all = false)
  )

  /** Десериализация распарсенных данных FreeAdvStatus, ранее сериализованных в JSON. */
  val deserialize: PartialFunction[Any, FreeAdvStatus] = {
    case jmap: ju.Map[_,_] =>
      FreeAdvStatus(
        isAllowed = Option(jmap get IS_ALLOWED_ESFN)
          .fold[Boolean] (false) (EsModel.booleanParser),
        when = Option(jmap get WHEN_ESFN)
          .fold (new DateTime(1970, 1, 1, 0, 0)) (EsModel.dateTimeParser),
        byUser = EsModel.stringParser(jmap get BY_USER_ESFN),
        reason = Option(jmap get REASON_ESFN) map EsModel.stringParser
      )
  }
}

/**
 * Экземпляр информации по результатам модерации карточки для бесплатного размещения.
 * @param isAllowed Разрешена ли карточка к эксплуатации?
 * @param when Когда было выставлено [не]разрешение?
 * @param byUser personId модератора sio.
 * @param reason Опциональное текстовое пояснение вердикта модератора.
 */
case class FreeAdvStatus(
  isAllowed : Boolean,
  byUser    : String,
  when      : DateTime = DateTime.now,
  reason    : Option[String] = None
) {
  import FreeAdvStatus._

  def toPlayJson: JsObject = {
    var acc: FieldsJsonAcc = List(
      IS_ALLOWED_ESFN -> JsBoolean(isAllowed),
      BY_USER_ESFN    -> JsString(byUser),
      WHEN_ESFN       -> EsModel.date2JsStr(when)
    )
    if (reason exists { !_.isEmpty })
      acc ::= REASON_ESFN -> JsString(reason.get)
    JsObject(acc)
  }
}


/** Аргументы для поиска карточек, подлежащий модерации или отмодерированных, передаются через
  * этот интерфейс. */
trait MdrSearchArgsI {
  def producerId: Option[String]
  def freeAdvIsAllowed: Option[Boolean]
  def maxResults: Int
  def offset: Int
}

