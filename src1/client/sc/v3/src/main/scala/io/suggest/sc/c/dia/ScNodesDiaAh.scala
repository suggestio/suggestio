package io.suggest.sc.c.dia

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Circuit, Effect, ModelRW}
import io.suggest.lk.m.CsrfTokenEnsure
import io.suggest.lk.nodes.MLknConf
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.lk.nodes.form.LkNodesFormCircuit
import io.suggest.lk.nodes.form.m.{BeaconsDetected, MLkNodesMode, MLkNodesModes, MLkNodesRoot, MTree, MTreeOuter, SetAd, TreeInit}
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.sc.Sc3Circuit
import io.suggest.sc.Sc3Module.sc3Circuit
import io.suggest.sc.m.{ScNodesBcnrSubscribeStatus, ScNodesModeChanged, ScNodesShowHide}
import io.suggest.sc.m.dia.MScNodes
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.DoNothing
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.2020 12:18
  * Description: sc-контроллер над формой узлов.
  */
class ScNodesDiaAh[M](
                       getNodesCircuit    : () => LkNodesFormCircuit,
                       modelRW            : ModelRW[M, MScNodes],
                       // в контроллере есть circuit.subscribe/unsubscribe() и куча обращений к zoom'ам.
                       sc3Circuit         : Sc3Circuit,
                     )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  /** Чтобы снижать нагрузку от перехвата и рендера маячковых сигналов,
    * надо вставить задержку указанной длительности. */
  def BEACONS_REACTION_DELAY_MS = 500

  /** Сборка экшена beacons detected. */
  def beaconDetectedAction(): BeaconsDetected = {
    val bcnsMap2 = sc3Circuit.beaconsRO.value
    BeaconsDetected( bcnsMap2 )
  }

  def handleBeaconsDetected(): Unit = {
    for (nodesCircuit <- value.circuit)
      nodesCircuit.dispatch( beaconDetectedAction() )
  }

  /** Эффект инициализации nodes circuit. */
  private def _nodesCircuitInitFx(nodesCircuit: Circuit[_]): Effect = {
    Effect.action {
      if (sc3Circuit.loggedInRO.value)
        nodesCircuit.dispatch( TreeInit() )

      // Отправляем текущий список маячков, даже пустой - тогда хотя бы будет создана подгруппа маячков в списке.
      if (sc3Circuit.platformRW.value.hasBle)
        nodesCircuit.dispatch( beaconDetectedAction() )

      DoNothing
    }
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Переключение видимости формы.
    case m: ScNodesShowHide =>
      val v0 = value

      if (m.visible) {
        v0.circuit.fold {
          val circuit = getNodesCircuit()

          var fx: Effect = if (sc3Circuit.csrfTokenRW.value.isEmpty) {
            CsrfTokenEnsure(
              onComplete = Some( m.toEffectPure ),
            ).toEffectPure
          } else {
            _nodesCircuitInitFx( circuit )
          }

          val focusedAdId = sc3Circuit.focusedAd
            .value
            .flatMap(_.id)
          val v2 = v0.copy(
            circuit = Some(circuit),
            // Сбрасывать adv-состояние, если текущая focused-карточка отсутствует:
            mode = focusedAdId
              .fold[MLkNodesMode]( MLkNodesModes.NodesManage )( _ => v0.mode ),
            // Дампим id текущекй сфокусированной карточки, чтобы застраховать юзера от фоновых изменений в плитке, приводящих к расфокусировке:
            focusedAdId = focusedAdId,
          )

          // Нужно подписаться на изменение данных по маячкам:
          fx += ScNodesBcnrSubscribeStatus().toEffectPure

          updated( v2, fx )

        } { nodesCircuit =>
          // Запуск эффекта инициализации начального дерева:
          val fx = _nodesCircuitInitFx( nodesCircuit )
          effectOnly( fx )
        }

      } else if (!m.visible && v0.circuit.nonEmpty) {
        val v2 = v0.copy(
          circuit = None,
          focusedAdId = None,
        )
        // Нужно закрыть подписку на маячки:
        val fx = ScNodesBcnrSubscribeStatus( Pot.empty.unavailable() ).toEffectPure
        updated( v2, fx )

      } else {
        logger.info( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = (m, v0.circuit) )
        noChange
      }


    // управление подпиской на события главной цепи Sc3Circuit:
    case m: ScNodesBcnrSubscribeStatus =>
      val v0 = value

      /** Если уже есть unsubscribe-функция, то отписаться от текущих событий: */
      def __unSubsCribeFx: Option[Effect] = for {
        oldF <- v0.unSubsCribeBcnr.toOption
      } yield {
        Effect.action {
          oldF()
          DoNothing
        }
      }

      if (m.unSubsCribeF.isEmpty && v0.unSubsCribeBcnr.isEmpty) {
        val pot1 = v0.unSubsCribeBcnr.pending()

        // Подписаться на события beaconer:
        val fx = Effect.action {
          var timerId = Option.empty[Double]

          val unSubsCribeF = sc3Circuit.subscribe( sc3Circuit.beaconsRO ) { _ =>
            if (timerId.isEmpty) {
              timerId = Some {
                DomQuick.setTimeout( BEACONS_REACTION_DELAY_MS ) { () =>
                  timerId = None
                  handleBeaconsDetected()
                }
              }
            }
          }

          ScNodesBcnrSubscribeStatus( pot1.ready(unSubsCribeF) )
        }

        val v2 = MScNodes.unSubsCribeBcnr.set( pot1 )(v0)
        updatedSilent( v2, fx )

      } else if (m.unSubsCribeF.isReady) {
        if (v0.unSubsCribeBcnr.nonEmpty)
          logger.log( ErrorMsgs.UNEXPECTED_NON_EMPTY_VALUE, msg = (m, v0.unSubsCribeBcnr) )

        // Дропнуть текущую unsubscribe-функцию, если она есть (хотя быть её не должно).
        val fxOpt = __unSubsCribeFx

        // Сохранение функции отписки в состояние:
        val v2 = (MScNodes.unSubsCribeBcnr set m.unSubsCribeF)(v0)
        ah.updatedSilentMaybeEffect(v2, fxOpt)

      } else if (m.unSubsCribeF.isUnavailable && v0.unSubsCribeBcnr.nonEmpty) {
        // Отписка от событий:
        val fxOpt = __unSubsCribeFx
        val v2 = (MScNodes.unSubsCribeBcnr set Pot.empty)(v0)

        ah.updatedSilentMaybeEffect( v2, fxOpt )

      } else {
        logger.info( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = (m, v0.unSubsCribeBcnr) )
        noChange
      }


    // Изменение режима формы.
    case m: ScNodesModeChanged =>
      val v0 = value

      (for {
        nodesCircuit <- v0.circuit
        if v0.mode !=* m.mode
      } yield {
        val v2 = (MScNodes.mode set m.mode)(v0)
        val fx = Effect.action {
          val adIdOpt2 = m.mode match {
            case MLkNodesModes.NodesManage =>
              None
            case MLkNodesModes.AdvInNodes =>
              v0.focusedAdId
          }
          nodesCircuit.dispatch( SetAd(adIdOpt2) )
          DoNothing
        }
        updated(v2, fx)
      })
        .getOrElse(noChange)

  }

}


object ScNodesDiaAh {

  def scNodesCircuitInit(userIsLoggedIn: Boolean): ActionResult[MLkNodesRoot] = {
    // Минимальное начальное состояние:
    val lknRoot = MLkNodesRoot(
      conf = MLknConf(
        onNodeId = sc3Circuit.inxStateRO.value.rcvrId,
        adIdOpt  = None,
      ),
      tree = MTreeOuter(
        // Для loggedIn-юзера сразу ставим pending, чтобы была крутилка - потом будет подгрузка узлов.
        tree = MTree.emptyNodesTree( isPending = userIsLoggedIn ),
      ),
    )

    ActionResult( Some(lknRoot), None )
  }

}
