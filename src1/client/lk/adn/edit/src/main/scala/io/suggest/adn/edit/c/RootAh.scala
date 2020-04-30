package io.suggest.adn.edit.c

import diode._
import io.suggest.adn.edit.api.ILkAdnEditApi
import io.suggest.adn.edit.m.{MAdnEditInternals, MAdnEditPopups, MLkAdnEditRoot, SaveResp}
import io.suggest.lk.m.{CloseAllPopups, DocBodyClick, HandleNewHistogramInstalled, Save}
import io.suggest.msg.ErrorMsgs
import io.suggest.n2.node.meta.MMetaPub
import io.suggest.primo.id._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.Log

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.04.18 21:35
  * Description: Контроллер внутренних фунцийи редактора ADN-узла.
  */
class RootAh[M](
                 api        : ILkAdnEditApi,
                 modelRW    : ModelRW[M, MLkAdnEditRoot]
               )
  extends ActionHandler( modelRW )
  with Log
{

  private def _root_internals_saving_LENS = {
    MLkAdnEditRoot.internals
      .composeLens( MAdnEditInternals.saving)
  }

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Реакция на кнопку сохранения формы.
    case m @ Save =>
      val v0 = value
      if (v0.internals.saving.isPending) {
        logger.log( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = m )
        noChange
      } else {
        val fx = Effect {
          api
            .save( v0.toForm )
            .transform { tryRes =>
              Success( SaveResp(tryRes) )
            }
        }

        val v2 = _root_internals_saving_LENS
          .modify( _.pending() )(v0)

        updated(v2, fx)
      }


    // Результат запроса сохранения.
    case m: SaveResp =>
      val v0 = value
      m.tryResp.fold(
        // Ошибка при запросе или на сервере.
        {ex =>
          val v2 = _root_internals_saving_LENS
            .modify( _.fail(ex) )(v0)

          updated(v2)
        },
        // Положительный ответ
        {form2 =>
          // Залить данные с сервера в форму.
          val v2 = (
            _root_internals_saving_LENS
              .modify(_.ready(form2)) andThen
            MLkAdnEditRoot.node.modify { node0 =>
              // Для ускорения и упрощения: Новые эджи не заливаем (в них нет fileSrv), а просто фильтруем существующие эджи по edge uid.
              val newEdgeIds = form2.edges
                .toIdIter
                .to( Set )

              node0.copy(
                meta    = form2.meta,
                edges   = node0.edges
                  .view
                  .filterKeys( newEdgeIds.contains )
                  .toMap,
                resView = form2.resView,
                errors  = MMetaPub.empty
              )
            }
          )(v0)

          updated( v2 )
        }
      )

    // Перехват ненужных событий.
    case _: HandleNewHistogramInstalled =>
      noChange

    // Закрытие всех попапов.
    case CloseAllPopups =>
      val v0 = value
      val v2 = (MLkAdnEditRoot.popups set MAdnEditPopups.empty)(v0)
      updated( v2 )

    // DocBodyClick используется для сокрытия color picker'а
    case DocBodyClick =>
      noChange

  }

}
