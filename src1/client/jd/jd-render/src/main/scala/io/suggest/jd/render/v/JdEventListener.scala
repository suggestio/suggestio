package io.suggest.jd.render.v

import io.suggest.jd.render.m.MJdRrrProps
import io.suggest.jd.tags.event.{MJdtEventActions, MJdtEventTypes}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, ReactEvent}
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.04.2021 14:10
  * Description: Утиль для работы с пользовательскими событиями карточек.
  */
object JdEventListener {

  @inline implicit def univEq: UnivEq[JdEventListener] = UnivEq.force


  /** Рендер необходимых TagMod для послушки необходимых событий.
    *
    * @param eventListener [[JdEventListener]]
    * @param eventActions Список экшенов.
    * @return TagMod.
    */
  def renderEventListener(eventListener: JdEventListener,
                          eventActions: Iterable[MJdtEventActions]): TagMod = {
    if (eventActions.isEmpty) {
      TagMod.empty

    } else {
      TagMod.fromTraversableOnce(
        for {
          eventAction <- eventActions.iterator
        } yield {
          eventAction.event.eventType match {
            case MJdtEventTypes.Click =>
              ^.onClick ==> eventListener.handleEvent( eventAction )
          }
        }
      )
    }
  }


  /** Отрендерить event listener'ы для тега. */
  def renderEventListener(state: MJdRrrProps): TagMod = {
    state.jdArgs.renderArgs.eventListener
      .fold( TagMod.empty )( renderEventListener(_, state.subTree.rootLabel.events.events) )
  }

}


/** Интерфейс для получения событий. */
trait JdEventListener {
  def handleEvent(eventAction: MJdtEventActions)(e: ReactEvent): Callback
}
