package models.mdr

import io.suggest.common.empty.EmptyUtil
import io.suggest.common.menum.EnumMaybeWithName
import util.FormUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.03.16 13:45
  * Description: Режимы отказа в размещении.
  * Используется как доп.опция в форме размещения для принятия экстренных решений.
  */
object MRefuseModes extends EnumMaybeWithName {

  /** Класс всех экземпляров этой модели. */
  protected[this] class Val(val strId: String)
    extends super.Val(strId)
  {
    /** Код по базе messages. */
    def nameI18n = "Refuse.mode." + strId
  }

  override type T = Val

  /** Удалять только запрашиваемое размещение. */
  val OnlyThis          : T = new Val("i")

  /** Удалять текущее размещение и отказывать в */
  val WithReqs          : T = new Val("req")

  /** Полное спиливание всех размещений, включая уже одобренные. */
  val WithAll           : T = new Val("all")


  def default = OnlyThis

  import play.api.data._, Forms._

  /** Опциональный маппинг для формы. */
  def mappingOpt: Mapping[Option[T]] = {
    val m = nonEmptyText(minLength = 1, maxLength = 10)
      .transform [String] (
        FormUtil.strTrimSanitizeLowerF,
        FormUtil.strIdentityF
      )
    optional(m)
      .transform [Option[T]] (
        { strIdOpt =>
          FormUtil.emptyStrOptToNone(strIdOpt)
            .flatMap(maybeWithName)
        },
        { _.map(_.strId) }
      )
  }

  /** Принудительный маппинг для формы. */
  def mapping: Mapping[T] = {
    mappingOpt
      .verifying("error.required", _.nonEmpty)
      .transform[T] (EmptyUtil.getF, EmptyUtil.someF)
  }

}
