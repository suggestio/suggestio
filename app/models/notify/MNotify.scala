package models.notify

import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model.{EnumMaybeWithName, EsModelPlayJsonT, EsModelT, EsModelStaticT}
import io.suggest.util.SioEsUtil._
import models.Context
import play.twirl.api.Html
import util.PlayMacroLogsImpl

import scala.collection.Map
import scala.concurrent.Future

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

  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): MNotify.T = {
    MNotify(
      id = id,
      versionOpt = version
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


case class MNotify(
  id: Option[String] = None,
  versionOpt: Option[Long] = None
) extends EsModelT with EsModelPlayJsonT {

  override def companion = MNotify
  override type T = this.type

  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    ???
  }

}


object ArgNames extends Enumeration with EnumMaybeWithName {
  protected class Val(strId: String) extends super.Val(strId)

  type ArgName = Val
  override type T = ArgName

  val AdnId     = new Val("a")
  //val PersonId  = new Val("p")
  val AdvId     = new Val("v")

}


// TODO Вынести в отд.файл. Тут будет много букв.
object NotifyTypes extends Enumeration with EnumMaybeWithName {

  protected abstract class Val(strId: String) extends super.Val(strId) {
    /**
     * Поиск необходимых данных
     * @param args
     * @param runtimeArgs
     * @param ctx
     * @return
     */
    def render(args: Map[String, Any], runtimeArgs: Map[String, Any])(implicit ctx: Context): Future[Html]
  }

  type NotifyType = Val
  override type T = NotifyType

  // TODO Разные
}
