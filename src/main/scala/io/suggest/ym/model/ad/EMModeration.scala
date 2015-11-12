package io.suggest.ym.model.ad

import io.suggest.model.es.{EsModelPlayJsonT, EsModelStaticMutAkvT, EsModelUtil}
import EsModelUtil.FieldsJsonAcc
import io.suggest.model.n2.extra.mdr.{MMdrExtra, MFreeAdv}
import io.suggest.util.SioEsUtil._
import io.suggest.ym.model.common.{EMReceivers, EMProducerId}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.{QueryBuilder, FilterBuilders, QueryBuilders}
import play.api.libs.json._
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

  def MDR_IS_ALLOWED_ESFN = MODERATION_ESFN + "." + MMdrExtra.FREE_ADV_ESFN + "." + MFreeAdv.IS_ALLOWED_ESFN

  /** MVEL-код определения рекламной карточки, у которой producer_id совпадает с одним из ресиверов.
    * Сначала скрипт проверяет наличие былой проверки и отфильтровывает, если уже выверено.
    * Затем ищем producerId в списке ресиверов, которые хоть что-то опубликовали. */
  val FREE_ADV_NEED_MDR_MVEL = {
    import EMProducerId.PRODUCER_ID_ESFN
    import EMReceivers._
    import MMdrExtra.FREE_ADV_ESFN, MFreeAdv.IS_ALLOWED_ESFN
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
        val fn = MDR_IS_ALLOWED_ESFN
        val f = FilterBuilders.termFilter(fn, freeAdvIsAllowed)
        QueryBuilders.filteredQuery(pqb, f)
      }
    } orElse {
      // producerId не задан. Нужно попытаться поискать по следующему параметру
      args.freeAdvIsAllowed.map { freeAdvIsAllowed =>
        val fn = MDR_IS_ALLOWED_ESFN
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
    val mapping = FieldObject(MODERATION_ESFN, enabled = true, properties = MMdrExtra.generateMappingProps)
    mapping :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (MODERATION_ESFN, jmap) =>
        acc.moderation = MMdrExtra.deserialize(jmap)
    }
  }

  /** Собрать саморазмещённые карточки, у которых не было модерации вообще. */
  def findSelfAdvNonMdr(args: MdrSearchArgsI)(implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    prepareSearch
      .setQuery( mdrSearchArgs2query(args) )
      .setSize(args.limit)
      .setFrom(args.offset)
      .execute()
      .map { searchResp2list }
  }
}



/** Интерфейс к полю с инфой по модерации. */
trait EMModerationI {
  def moderation: MMdrExtra
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
  var moderation: MMdrExtra
}


/** Аргументы для поиска карточек, подлежащий модерации или отмодерированных, передаются через
  * этот интерфейс. */
trait MdrSearchArgsI {
  def producerId: Option[String]
  /**
    * @return None -- ищем немодерированные карточки.
    *         Some(x) ищем отмодерированные с isAllowed == x.
    */
  def freeAdvIsAllowed: Option[Boolean]
  def limit: Int
  def offset: Int
}

