package io.suggest.bill.tf.daily

import boopickle.Default._
import io.suggest.bill.Amount_t
import io.suggest.i18n.MsgCodes
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.17 16:45
  * Description: Модель режимов тарификации размещения на узле.
  */
object ITfDailyMode {

  /** Поддержка boopickle-сериализации. */
  implicit val tfDailyModePickler: Pickler[ITfDailyMode] = {
    compositePickler[ITfDailyMode]
      .addConcreteType[InheritTf.type]
      .addConcreteType[ManualTf]
  }


  implicit def univEq: UnivEq[ITfDailyMode] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

  implicit def iTfDailyModeFormat: OFormat[ITfDailyMode] = {
    val manFmt = ManualTf.manualTfFormat
    val reads = manFmt
      .map[ITfDailyMode](identity)
      .orElse(
        Reads
          .pure(InheritTf)
          .map[ITfDailyMode](identity)
      )
    val owrites = OWrites[ITfDailyMode] {
      case m: ManualTf => manFmt.writes(m)
      case InheritTf   => JsObject.empty
    }
    OFormat(reads, owrites)
  }

}


/** Интерфейс-маркер элементов модели. */
sealed trait ITfDailyMode {

  def msgCode: String

  def isValid: Boolean

  /** Amount, если задан. Появился из-за особенностей валидации через accord. */
  def amountOpt: Option[Amount_t] = None

  def isManual: Boolean
  def isInherited: Boolean

  /** Инстанс ManualTf, если актуально. */
  def manualOpt: Option[ManualTf]

  def withAmount(amount: Amount_t) = ManualTf(amount = amount)

}


/** Режим: наследовать тариф. */
case object InheritTf extends ITfDailyMode {
  override def manualOpt = None
  override def isValid = true
  override def isManual = false
  override def isInherited = true
  override def msgCode = MsgCodes.`Inherited`
}


/** Ручное выставление тарифа.
  *
  * @param amount Базовая ставка тарифа, задаваемая юзером.
  */
final case class ManualTf( amount: Amount_t )
  extends ITfDailyMode
{

  override def msgCode = MsgCodes.`Set.manually`
  override def amountOpt = Some(amount)
  override def manualOpt = Some(this)
  override def isManual = true
  override def isInherited = false

  override def withAmount(amount2: Amount_t): ManualTf = {
    copy(amount = amount2)
  }

  override def isValid: Boolean = {
    amount >= TfDailyConst.Amount.MIN &&
    amount <= TfDailyConst.Amount.MAX
  }

}
object ManualTf {

  implicit def manualTfFormat: OFormat[ManualTf] = {
    (__ \ "a").format[Amount_t]
      .inmap[ManualTf](apply, _.amount)
  }

}
