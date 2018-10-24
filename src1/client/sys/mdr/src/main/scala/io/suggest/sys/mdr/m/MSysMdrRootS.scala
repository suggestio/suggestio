package io.suggest.sys.mdr.m

import diode.FastEq
import diode.data.Pot
import io.suggest.jd.render.v.JdCss
import io.suggest.sys.mdr.{MMdrActionInfo, MMdrConf, MMdrNextResp}
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.10.18 22:04
  * Description: Корневая модель состояния компонента модерации карточек.
  */
object MSysMdrRootS {

  /** Поддержка FastEq для корневой модели. */
  implicit object MSysMdrRootSFastEq extends FastEq[MSysMdrRootS] {
    override def eqv(a: MSysMdrRootS, b: MSysMdrRootS): Boolean = {
      (a.jdCss ===* b.jdCss) &&
      (a.info ===* b.info) &&
      (a.dialogs ===* b.dialogs) &&
      (a.mdrPots ===* b.mdrPots) &&
      (a.fixNodePots ===* b.fixNodePots) &&
      (a.nodeOffset ==* b.nodeOffset) &&
      (a.conf ===* b.conf)
    }
  }

  @inline implicit def univEq: UnivEq[MSysMdrRootS] = UnivEq.derive

}


/** Корневой контейнер данных состояния react-формы sys-mdr.
  *
  * @param conf Конфиг формы.
  * @param jdCss Стили рендера рекламной карточки.
  * @param info Ответ сервера с данными для модерации.
  * @param dialogs Состояния диалогов.
  * @param mdrPots Состояния элементов модерации.
  * @param nodeOffset Сдвиг среди модерируемых узлов.
  *                   Позволяет пропустить узел, или вернуться назад к пропущенному узлу.
  *                   В норме - ноль.
  *                   Списки ошибкок узлов с сервера должны инкрементить это значение.
  * @param fixNodePots Запросы ремонта.
  */
case class MSysMdrRootS(
                         jdCss      : JdCss,
                         info       : Pot[MMdrNextResp]                     = Pot.empty,
                         dialogs    : MMdrDialogs                           = MMdrDialogs.empty,
                         mdrPots    : Map[MMdrActionInfo, Pot[None.type]]   = Map.empty,
                         fixNodePots: Map[String, Pot[None.type]]           = Map.empty,
                         nodeOffset : Int                                   = 0,
                         conf       : MMdrConf,
                       ) {

  def withJdCss( jdCss: JdCss ) = copy(jdCss = jdCss)
  def withInfo( info: Pot[MMdrNextResp] ) = copy(info = info)
  def withDialogs( dialogs: MMdrDialogs ) = copy(dialogs = dialogs)
  def withMdrPots( mdrPots: Map[MMdrActionInfo, Pot[None.type]] ) = copy(mdrPots = mdrPots)
  def withNodeOffset(nodeOffset: Int) = copy(nodeOffset = nodeOffset)
  def withFixNodePots( fixNodePots: Map[String, Pot[None.type]] ) = copy(fixNodePots = fixNodePots)

}
