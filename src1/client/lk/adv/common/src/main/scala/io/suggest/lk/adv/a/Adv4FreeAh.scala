package io.suggest.lk.adv.a

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.adv.free.MAdv4Free
import io.suggest.lk.adv.m.SetAdv4Free
import japgolly.univeq._

/**
  * Diode Action handler для реакции на галочку бесплатного размещения для суперюзеров.
  */
class Adv4FreeAh[M](
                     modelRW        : ModelRW[M, Option[MAdv4Free]],
                     priceUpdateFx  : Effect
                   )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case m: SetAdv4Free =>
      (for {
        a4f <- value
        if (m.checked !=* a4f.checked)
      } yield {
        val v2 = Some( (MAdv4Free.checked set m.checked)(a4f) )
        // TODO Если checked сменился с false на true, то надо бы просто занулять отображаемую цену без дёрганья сервера.
        val fx = priceUpdateFx
        updated( v2, fx )
      })
        .getOrElse( noChange )

  }

}
