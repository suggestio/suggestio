package io.suggest.sc.sjs.m.msc.fsm

import io.suggest.sc.sjs.m.SafeWnd
import io.suggest.sjs.common.util.SjsLogger
import org.scalajs.dom.PopStateEvent

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.06.15 12:19
 * Description: Модель для управления состояниями системы и переключения между ними.
 * Для трансформации состояний используются case class .copy(),
 * для применения данных состояния -- выполнение действий, связанных с изменившимися полями.
 */
object MScFsm extends SjsLogger {

  /** Хранилище модели. */
  private var _storage: Storage = Storage()

  /** При перемотке истории необходимо оставить запас для возврата вперёд по истории. */
  private var _snapshot: Option[Storage] = None

  /** Забыть ранее сделаный snapshot. */
  private def forgetSnapshot(): Unit = {
    if (_snapshot.nonEmpty)
      _snapshot = None
  }

  /** Убедиться, что snapshot уже сделан. */
  private def ensureSnapshot(): Unit = {
    if (_snapshot.isEmpty)
    _snapshot = Some(_storage)
  }

  private def snapshotOrStorage: Storage = _snapshot getOrElse _storage

  /** Подписка модели на события истории. Вызывается при запуске приложения. */
  def subscribeEvents(): Unit = {
    SafeWnd.addEventListener("popstate") { e: PopStateEvent =>
      val index = e.state.asInstanceOf[HStatePtr].index
      go(index)
    }
  }

  /** Текущее состояние. */
  def state = _storage.states.head

  /** Предыдущее состояние, если есть */
  def popStateOpt = _storage.states.tail.headOption

  /** Заменить текущее состояние новым. */
  def replaceState(newState: MScState): Unit = {
    _storage = _storage.copy(
      states = newState :: _storage.states.tail
    )
    forgetSnapshot()
  }

  /**
   * Обновить текущее с помощью фунцкии, породив новое состояние и добавив в его в стек состояний.
   * @param f Функция обновления состояния.
   * @return Результат f(state).
   */
  def transformState(f: MScState => MScState): MScState = {
    val newState = f(state)
    pushState(newState)
    newState
  }

  def go(i: Int): Unit = {
    val storage = snapshotOrStorage
    val i1 = storage.index - i
    if (i1 >= 0) {
      ensureSnapshot()
      _storage = _storage.copy(
        states = storage.states.drop(i1),
        index  = i
      )
    } else {
      warn("State#" + i + " not found")
    }
  }

  /** Найти состояние с указанным порядковым номером. */
  def get(i: Int): Option[MScState] = {
    val storage = snapshotOrStorage
    val i1 = storage.index - i
    if (i1 < 0) {
      None
    } else {
      Some( storage.states(i1) )
    }
  }

  /** Добавить новое состояние в стек. */
  def pushState(newState: MScState): Unit = {
    _storage = _storage.copy(
      states = newState :: _storage.states,
      index  = _storage.index + 1
    )
    for (hapi <- SafeWnd.history) {
      // TODO Брать заголовок откуда-то.
      hapi.pushState(HStatePtr(_storage.index), "Suggest.io")
    }
    forgetSnapshot()
  }

  /** Откатить текущее состояние на шаг назад, если возможно. */
  def rollbackState(): Unit = {
    ensureSnapshot()
    val states = _storage.states
    if (states.nonEmpty) {
      val tl = states.tail
      if (tl.nonEmpty) {
        _storage = _storage.copy(
          states = tl,
          index  = _storage.index -1
        )
      }
    }
  }

  /** Накатить изменения между текущим и предыдущим состоянием. */
  def applyStateChanges(): Unit = {
    for(prevState <- popStateOpt) {
      applyStateChanges(state, prevState)
    }
  }
  /** Перевести выдачу в новое состояние, проанализировав изменения между новым и текущим состоянием. */
  def applyStateChanges(state: MScState, prevState: MScState): Unit = {
    state.applyChangesSince(prevState)
  }


  /** Внутренее хранилище модели. */
  private case class Storage(
    states : List[MScState] = List( MScState() ),
    index  : Int = 0
  ) {
    override def toString: String = "StSt(" + index + ")"
  }
  
  /** Модель сохраняемой в History API информации: порядковый номер в _states с конца. */
  sealed trait HStatePtr extends js.Object {
    @JSName("i")
    var index: Int = js.native
  }
  object HStatePtr {
    def apply(index: Int): HStatePtr = {
      val d = js.Dictionary[js.Any](
        "i" -> index
      )
      d.asInstanceOf[HStatePtr]
    }
  }

}
