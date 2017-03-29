package io.suggest.lk.nodes.form.a.menu

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.lk.nodes.form.m.{DocumentClick, MNodeMenuS, NodeMenuBtnClick}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.03.17 15:23
  * Description: Action handler для менюшки узла.
  */
class NodeMenuAh[M](
                     modelRW  : ModelRW[M, Option[MNodeMenuS]]
                   )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по кнопке меню узла.
    case NodeMenuBtnClick =>
      val v2 = value.fold[Option[MNodeMenuS]] {
        Some(MNodeMenuS())
      } { _ =>
        None
      }
      updated( v2 )

    // Клик где-то в документе.
    case DocumentClick =>
      // Смотреть только текущее значение из модели, чтобы не скрыть только что открытое меню.
      val v0 = modelRW()
      if (v0.nonEmpty) {
        updated(None)
      } else {
        noChange
      }

  }

}
