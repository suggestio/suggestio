package models.notify

import io.suggest.model.EsModel.{FieldsJsonAcc, stringParser}
import io.suggest.model._
import io.suggest.util.SioEsUtil._
import org.joda.time.DateTime
import play.api.libs.json.{JsBoolean, JsString}
import util.PlayMacroLogsImpl
import util.notify.NotifyTypes
import util.notify.NotifyTypes.NotifyType

import scala.collection.Map

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.01.15 19:46
 * Description: Модель, описывающая нотификации для узла или другого объекта системы suggest.io.
 */
object MNotify extends EsModelStaticT with PlayMacroLogsImpl {

  override type T = MNotify
  override val ES_TYPE_NAME = "ntf"

  val NOTIFY_TYPE_ESFN  = "nt"
  val OWNER_ID_ESFN     = "ownerId"     // Такая же, как в MMartCategory
  val ARGS_ESFN         = "args"
  val DATE_CREATED_ESFN = "dc"
  val IS_CLOSEABLE_ESFN = "ic"
  val IS_UNSEEN_ESFN    = "iu"

  
  def isCloseableDflt = true
  def isUnseenDflt    = true
  def dateCreatedDflt = DateTime.now()
  def argsDflt        = EmptyArgsInfo

  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    val ntype = Option(m get NOTIFY_TYPE_ESFN)
      .map(stringParser)
      .flatMap(NotifyTypes.maybeWithName)
      .get
    MNotify(
      ntype       = ntype,
      ownerId     = stringParser(m get OWNER_ID_ESFN),
      argsInfo    = ntype.deserializeArgsInfo(m get ARGS_ESFN),
      dateCreated = EsModel.dateTimeParser(m get DATE_CREATED_ESFN),
      isCloseable = Option(m get IS_CLOSEABLE_ESFN).fold(isCloseableDflt)(EsModel.booleanParser),
      isUnseen    = Option(m get IS_UNSEEN_ESFN).fold(isUnseenDflt)(EsModel.booleanParser),
      id          = id,
      versionOpt  = version
    )
  }

  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldSource(enabled = true),
      FieldAll(enabled = true)
    )
  }

  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(NOTIFY_TYPE_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(OWNER_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldObject(ARGS_ESFN, properties = Nil, enabled = false),
      FieldDate(DATE_CREATED_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = false),
      FieldBoolean(IS_CLOSEABLE_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldBoolean(IS_UNSEEN_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    )
  }

}

// TODO Не забыть прилинковать эту модель к SiowebEsModel!

import MNotify._


/** Класс-экземпляр одной нотификации. */
case class MNotify(
  ntype         : NotifyType,
  ownerId       : String,
  argsInfo      : IArgsInfo       = MNotify.argsDflt,
  dateCreated   : DateTime        = MNotify.dateCreatedDflt,
  isCloseable   : Boolean         = MNotify.isCloseableDflt,
  isUnseen      : Boolean         = MNotify.isUnseenDflt,
  id            : Option[String]  = None,
  versionOpt    : Option[Long]    = None
) extends EsModelT with EsModelPlayJsonT {

  override def companion = MNotify
  override type T = this.type

  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    var acc: FieldsJsonAcc = List(
      NOTIFY_TYPE_ESFN  -> JsString(ntype.strId),
      OWNER_ID_ESFN     -> JsString(ownerId),
      DATE_CREATED_ESFN -> EsModel.date2JsStr(dateCreated)
    )
    if (argsInfo.nonEmpty)
      acc ::= ARGS_ESFN -> argsInfo.toPlayJson
    if (isCloseable != isCloseableDflt)
      acc ::= IS_CLOSEABLE_ESFN -> JsBoolean(isCloseable)
    if (isUnseen != isUnseenDflt)
      acc ::= IS_UNSEEN_ESFN -> JsBoolean(isUnseen)
    acc
  }

}


