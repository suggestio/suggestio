package io.suggest.lk.adv.a

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.lk.adv.m.SetAdv4Free

/**
  * Diode Action handler для реакции на галочку бесплатного размещения для суперюзеров.
  * Зуммировать доступ желательно прямо до поля галочки.
  */
class Adv4FreeAh[M](
                     modelRW        : ModelRW[M, Option[Boolean]],
                     priceUpdateFx  : Effect
                   )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {
    case e: SetAdv4Free =>
      val checked0 = value.contains(true)
      val checked2 = e.checked
      if (checked0 != checked2) {
        // TODO Если checked сменился с false на true, то надо бы просто занулять отображаемую цену без дёрганья сервера.
        updated( Some(checked2), priceUpdateFx )
      } else {
        noChange
      }
  }

}
