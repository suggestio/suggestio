package models.adv

import io.suggest.common.menum.EnumValue2Val
import models.event.{MEventTypes, MEventType}

/** Статическая модель, описывающая разновидности размещений. */
object MAdvModes extends EnumValue2Val {

  protected[this] trait ValT {
    /** Строковой id типа. */
    def strId: String

    /** Тип события, сопутствующего этому экземпляру. */
    def eventType: MEventType

    /**
     * Владелец порождаемого события. Это receiver для req, и producer для ok и refused.
     * @param adv Абстрактное размещение.
     * @return adnId владельца события.
     */
    def eventOwner(adv: MAdvI): String

    /**
     * AdnId узла-источника события.
     * @param adv Абстрактное размещение.
     * @return AdnId узла.
     */
    def eventSource(adv: MAdvI): String
  }

  /** Быстрый mixin для req и ok размещений, говорящий системе, что owner'ом события является продьюсер. */
  protected[this] trait EvtOwnerIsProd extends ValT {
    override def eventOwner(adv: MAdvI): String = adv.prodAdnId
    override def eventSource(adv: MAdvI): String = adv.rcvrAdnId
  }

  protected abstract class Val(val strId: String)
    extends super.Val(strId)
    with ValT

  type T = Val

  /** Заапрувленное размещение. */
  val OK: T = new Val("o") with EvtOwnerIsProd {
    override def eventType = MEventTypes.AdvOutcomingOk
  }

  /** Запрос размещения. */
  val REQ: T = new Val("r") {
    override def eventType = MEventTypes.AdvReqIncoming
    override def eventOwner(adv: MAdvI) = adv.rcvrAdnId
    override def eventSource(adv: MAdvI) = adv.prodAdnId
  }

  /** Отклонённое размение. */
  val REFUSED: T = new Val("e") with EvtOwnerIsProd {
    override def eventType = MEventTypes.AdvOutcomingRefused
  }

  def busyModes: Set[T] = Set(OK, REQ)
}
