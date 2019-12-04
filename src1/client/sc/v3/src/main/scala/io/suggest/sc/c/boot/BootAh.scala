package io.suggest.sc.c.boot

import diode._
import diode.data.Pot
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.dev.MPlatformS
import io.suggest.maps.m.RcvrMarkersInit
import io.suggest.msg.WarnMsgs
import io.suggest.sc.Sc3Circuit
import io.suggest.sc.c.dia.FirstRunDialogAh
import io.suggest.sc.m.boot._
import io.suggest.sc.m.dia.InitFirstRunWz
import io.suggest.sc.m._
import io.suggest.sc.m.in.MJsRouterS
import io.suggest.sc.m.search.MGeoTabData
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.dom.DomQuick
import io.suggest.spa.CircuitUtil
import io.suggest.spa.DiodeUtil.Implicits._

import scala.concurrent.{Future, Promise}
import scala.util.{Success, Try}
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.01.19 12:20
  * Description: Контроллер этапности инициализации выдачи.
  *
  * Ранее, управление инициалиацией проходило прямо в Sc3Circuit,
  * но тот страшный код стал слишком сложным и немасштабируемым.
  */

class BootAh[M](
                 modelRW    : ModelRW[M, MScBoot],
                 circuit    : Sc3Circuit,
               )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  object Services {

    /** Запуск и обработка вокруг pot'а.
      *
      * @param serviceId id сервиса.
      * @param zoomR Функция для zoom'а.
      * @tparam X Неважный тип для zoomR.
      * @return Собранный эффект.
      */
    private def _startWithPot[X: IValueReadySubscriber](serviceId   : MBootServiceId,
                                                        zoomR       : ModelR[MScRoot, X],
                                                        timeoutOkMs : Option[Int] = None): Future[BootStartCompleted] = {
      val doneP = Promise[ReadyInfo_t]()

      // Если задан ok-таймаут, то повесить макс.время на ожидание, завершающийся успехом:
      val tpOkOpt = for (timeoutMs <- timeoutOkMs) yield {
        val tpOk = DomQuick.timeoutPromise( timeoutMs )
        doneP.completeWith( tpOk.fut )
        tpOk
      }

      // Мониторить готовность js-роутера:
      val unsubscribeF = circuit.subscribe( zoomR ) { jsRouterRO =>
        if (!doneP.isCompleted) {
          val v = jsRouterRO.value
          implicitly[IValueReadySubscriber[X]]
            .maybeCompletePromise( doneP, v )
          // Возможно, Promise был исполнен строкой выше. И если это так, то отменить таймаут закрытия фьючерса:
          if (doneP.isCompleted)
            for (tp <- tpOkOpt)
              DomQuick.clearTimeout( tp.timerId )
        }
      }

      // Конверсия результата
      doneP
        .future
        .transform { tryRes =>
          Try( unsubscribeF() )
          val action = BootStartCompleted( serviceId, tryRes )
          Success( action )
        }
    }


    /** Описание процедуры js-роутера. */
    class JsRouterSvc extends IBootService {

      override def serviceId = MBootServiceIds.JsRouter

      override def startFx: Effect = {
        val bootSvcFx = Effect {
          val z = CircuitUtil.mkLensZoomRO( circuit.jsRouterRW, MJsRouterS.jsRouter )
          _startWithPot( serviceId, z )
        }
        val bgFx = JsRouterInit.toEffectPure
        bootSvcFx + bgFx
      }
      // TODO Sc3Main содержит свой запуск jsRouter-инициализации, который ещё активирует RmeLogging.

    }


    /** Спека инициализации карты ресиверов. */
    class RcvrsMapSvc extends IBootService {

      override def serviceId = MBootServiceIds.RcvrsMap

      override def depends =
        MBootServiceIds.JsRouter :: super.depends

      override def startFx: Effect = {
        val bgFx = RcvrMarkersInit.toEffectPure
        val bootSvcFx = Effect {
          val z = CircuitUtil.mkLensZoomRO( circuit.geoTabDataRW, MGeoTabData.rcvrsCache )
          _startWithPot( serviceId, z )
        }
        bootSvcFx + bgFx
      }

    }


    /** Спека инициализации платформы. */
    class PlatformSvc extends IBootService {

      override def serviceId = MBootServiceIds.Platform

      override def startFx: Effect = {
        Effect {
          val mroot = circuit.rootRW.value
          val plat = mroot.dev.platform

          if (plat.isCordova) {
            // cordova - Нужно повесится на platform-ready.
            val isReadyRO = CircuitUtil.mkLensZoomRO(circuit.platformRW, MPlatformS.isReady)
            _startWithPot( serviceId, isReadyRO, timeoutOkMs = Some(7000) )
          } else {
            // browser - сразу вернуть ответ.
            Future.successful( this.finished() )
          }
        }
      }

    }


    /** Сбор данных геолокации для первого запуска. */
    class GeoLocDataAccSvc extends IBootService {
      override def serviceId = MBootServiceIds.GeoLocDataAcc
      override def startFx = BootLocDataWz.toEffectPure
    }

    /** Унифицированная сборка сервисов. */
    def make(id: MBootServiceId): IBootService = {
      id match {
        case MBootServiceIds.JsRouter               => new JsRouterSvc
        case MBootServiceIds.RcvrsMap               => new RcvrsMapSvc
        case MBootServiceIds.Platform               => new PlatformSvc
        case MBootServiceIds.GeoLocDataAcc          => new GeoLocDataAccSvc
      }
    }

  }


  /** Аккамулятор для выполнения запуска какой-либо службы с её зависимостями.
    *
    * @param v0 Значение boot-модели.
    * @param restTargets Список целей запуска, которые надо отработать.
    * @param fxAcc Аккамулятор эффектов запуска.
    * @param prevTargets Предыдущие уже пройденные цели инициализации, чтобы избегать бесконечного зацикливания на кольцах.
    * @param levelStartCompleted На текущем уровне все зависимости уже запущены?
    * @param stepsCounter Счётчик шагов, чтобы прерывать аномальное зацикливание.
    */
  private case class StartAcc(
                               v0                   : MScBoot,
                               restTargets          : List[MBootServiceId],
                               fxAcc                : List[Effect]            = Nil,
                               prevTargets          : Set[MBootServiceId]     = Set.empty,
                               levelStartCompleted  : Boolean                 = true,
                               stepsCounter         : Int                     = 0,
                             )

  /** Процедура запуска одного сервиса. */
  private def _processServiceStart( acc0: StartAcc ): StartAcc = {
    acc0.restTargets match {

      // Слишком много уровней зависимостей.
      case _ if acc0.stepsCounter > 100 =>
        throw new IllegalStateException(
          acc0.stepsCounter.toString + HtmlConstants.SPACE + acc0.restTargets.mkString(HtmlConstants.COMMA)
        )

      // Есть хотя бы одна цель для инициализации.
      case svcId :: restTargetsTl =>
        val svcDataOpt0 = acc0.v0.services.get( svcId )

        val svcStartCompleted = svcDataOpt0.exists( _.isStarted )

        val prevTargetsHasSvc = (acc0.prevTargets contains acc0.restTargets.head)
        if (
          // Эта служба уже [была] запущена, возможно уже работает?
          svcStartCompleted || prevTargetsHasSvc
        ) {
          // Эта цель инициализации уже была пройдена. Перейти к следующей цели.
          val acc2 = acc0.copy(
            restTargets         = restTargetsTl,
            stepsCounter        = acc0.stepsCounter + 1,
            // Всё ли запущено на данном уровне зависимостей?
            levelStartCompleted = acc0.levelStartCompleted  &&  svcDataOpt0.exists( _.isStartCompleted )
          )
          _processServiceStart( acc2 )

        } else {
          // Служба ещё не запускалась вообще. Может быть это из-за зависимостей?
          val svcData0 = svcDataOpt0
            .getOrElse( MBootServiceState( Services.make(svcId)) )

          val depIds = svcData0.tg.depends
          val acc1 = acc0.copy(
            restTargets         = depIds,
            stepsCounter        = acc0.stepsCounter + 1,
            levelStartCompleted = true,
            // Сразу запихнуть текущий сервис в акк, чтобы не было циклов на под-уровнях.
            prevTargets         = acc0.prevTargets + svcId,
          )
          val acc2 = _processServiceStart( acc1 )

          val isWillStart = acc2.levelStartCompleted

          //println( svcId, isWillStart, acc2.levelStartCompleted )

          // Если все зависимости отработаны, то можно запустить текущую службу
          val fxAcc2 = if (isWillStart)
            svcData0.tg.startFx :: acc2.fxAcc
          else
            acc2.fxAcc

          val svcData2 = if (isWillStart)
            MBootServiceState.started.set( Some(Success(false)) )(svcData0)
          else
            svcData0

          // Эффект запуска на руках. Закинуть в fx-акк и перейти на следующую итерацию:
          val acc3 = acc2.copy(
            restTargets   = restTargetsTl,
            fxAcc         = fxAcc2,
            stepsCounter  = acc2.stepsCounter + 1,
            levelStartCompleted = {
              acc0.levelStartCompleted &&
              acc2.levelStartCompleted &&
              svcData2.isStartCompleted
            },
            // Залить в состояние обновлённые данные по сервису:
            v0 = if (isWillStart) {
              acc2.v0.copy(
                services = acc2.v0.services + (svcId -> svcData2),
                // Если сервис в списке main-целей, то удалить его оттуда, т.к. запущен.
                targets =
                  if (acc2.v0.targets contains svcId)
                    acc2.v0.targets
                      .filter(_ !=* svcId)
                  else
                    acc2.v0.targets
              )
            } else {
              // Не было запуска текущей службы - нечего тут запускать.
              acc2.v0
            },
          )

          _processServiceStart(acc3)
        }

      // Больше нет целей для инициализации на этом уровне. Завершаемся.
      case Nil =>
        acc0

    }
  }


  private def _processStartState( v0: MScBoot, svcIds: List[MBootServiceId] ): ActionResult[M] = {
    val startAcc2 = _processServiceStart(
      StartAcc(
        v0          = v0,
        restTargets = svcIds,
      )
    )

    val fxOpt = startAcc2.fxAcc.mergeEffects

    ah.updatedMaybeEffect( startAcc2.v0, fxOpt )
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал к запуску какого-либо процесса.
    case m: Boot =>
      val v0 = value

      // Нужно залить в состояние список запрошенных целей, которые ещё не запущены:
      val v1 = MScBoot.targets.modify(_ ++ m.svcIds)(v0)
      _processStartState( v1, m.svcIds )


    // Сигнал завершения какого-то этапа.
    case m: BootStartCompleted =>
      // У какой-то запускаемой службы завершился start-эффект. Залить данные в состояние.
      val v0 = value
      v0.services
        .get( m.svcId )
        .fold {
          LOG.warn( WarnMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
          noChange
        } { svcData0 =>
          val svcData2 = MBootServiceState.started.set(
            Some(
              m.tryRes
                .map(_ => true)
            )
          )(svcData0)

          // Залить полученную инфу в состояние.
          val v1 = v0.copy(
            services = v0.services + (m.svcId -> svcData2),
            targets  = if (v0.targets contains m.svcId)
              v0.targets - m.svcId
            else
              v0.targets
          )

          if (v1.targets.nonEmpty) {
            _processStartState( v1, v1.targets.toList )
          } else {
            val fx = _reRouteFx( allowRouteTo = true )
            updatedSilent(v1, fx)
          }
        }


    // Сигнал к запуску сбора данных геолокации, прав на геолокацию, и т.д.
    case BootLocDataWz =>
      if (FirstRunDialogAh.isNeedWizardFlow()) {
        // Нет уже заданных гео-данных, и требуется запуск мастера.
        // Текущий эффект передаёт управление в WizardAh, мониторя завершение визарда.
        val initFirstWzFx = InitFirstRunWz(true).toEffectPure
        val afterInitFx = BootLocDataWzAfterInit.toEffectPure

        // Выставить в состояние факт запуска wzFirst:
        val v2 = (MScBoot.wzFirstDone set OptionUtil.SomeBool.someFalse)(value)
        val fx = initFirstWzFx >> afterInitFx

        updatedSilent(v2, fx)

      } else {
        // Мастер 1го запуска не требуется, а геолокация нужна. Переходим на следующих шаг:
        _afterWzDone( runned = false, started = false )
      }


    // После init-wz-вызова произвести анализ результатов выполненной деятельности.
    case m @ BootLocDataWzAfterInit =>
      val startedAtMs = System.currentTimeMillis()
      val firstRO = circuit.firstRunDiaRW
      if (firstRO.value.view.isEmpty) {
        // Почему-то не был запущен wizard, хотя должен был быть, т.к. isNeedWizardFlow() вернул true.
        LOG.warn( WarnMsgs.INIT_FLOW_UNEXPECTED, msg = m )
        _afterWzDone( Some(startedAtMs), started = true, runned = false )

      } else {
        // Есть какая-то деятельность в визарде. Подписаться
        val fx = Effect {
          val p = Promise[None.type]()
          // Состояние wizard'а инициализировано, значит wizard запущен, дождаться завершения мастера.
          val wizardWatcherUnSubscribeF = circuit.subscribe(firstRO) { diaFirstProxy =>
            if (diaFirstProxy.value.view.isEmpty)
              p.trySuccess(None)
          }
          p.future
            .andThen { case _ =>
              wizardWatcherUnSubscribeF()
            }
            .transform { _ =>
              Success( BootLocDataWzAfterWz(startedAtMs) )
            }
        }
        effectOnly(fx)
      }


    // Сигнал о завершении визарда:
    case m: BootLocDataWzAfterWz =>
      _afterWzDone( Some(m.startedAtMs), runned = true, started = true )

  }

  private def _reRouteFx(allowRouteTo: Boolean): Effect = {
    Effect.action {
      circuit.internalsRW
        .value
        .info.currRoute
        .filter { _ => allowRouteTo }
        .fold[IScRootAction](ResetUrlRoute)(RouteTo.apply)
    }
  }

  /** Мастер уже завершился или не запускался и не планирует. Быстро или медленно.
    * Но суть исходная - заняться получением гео.данных, если их ещё нет.
    */
  private def _afterWzDone(wzStartedAtMs: Option[Long] = None, runned: Boolean = false, started: Boolean = false, v0: MScBoot = value): ActionResult[M] = {
    //println( s"_afterWzDone(${wzStartedAtMs.orNull}): Need geo loc? route=" + circuit.internalsRW.value.info.currRoute )

    // Тут несколько вариантов:
    // - гео-данные уже накоплены
    // - надо подождать геоданных сколько-то времени до накопления или таймаута.
    // Но пока на это всё плевать, т.к. код для просто упорядоченного запуска уже получился слишком сложный.

    // Экшен, сигнализирующий о завершении запуска сервиса:
    val doneMyselfFx = _svcStartDoneAction( MBootServiceIds.GeoLocDataAcc ).toEffectPure

    val v2 = (MScBoot.wzFirstDone set OptionUtil.SomeBool.someTrue)(v0)
    // TODO Если диалог не открывался, но вызывался, то надо запустить геолокацию вручную.
    val fx = doneMyselfFx >> _reRouteFx(
      allowRouteTo = !(started || runned)
    )

    updated(v2, fx)
  }


  private def _svcStartDoneAction(svc: MBootServiceId) =
    BootStartCompleted( svc, Success(None) )

}


sealed trait IValueReadySubscriber[T] {
  def maybeCompletePromise( readyPromise: Promise[ReadyInfo_t], value: T ): Unit
}

object IValueReadySubscriber {

  implicit def identitySubscriber: IValueReadySubscriber[Boolean] = {
    new IValueReadySubscriber[Boolean] {
      override def maybeCompletePromise(readyPromise: Promise[ReadyInfo_t], value: Boolean): Unit =
        if (value)
          readyPromise.trySuccess( None )
    }
  }

  implicit def PotSubscriber[T]: IValueReadySubscriber[Pot[T]] = {
    new IValueReadySubscriber[Pot[T]] {
      override def maybeCompletePromise(readyPromise: Promise[ReadyInfo_t], pot: Pot[T]): Unit = {
        if (!pot.isPending) {
          for (_ <- pot)
            readyPromise.trySuccess( None )
          for (ex <- pot.exceptionOption)
            readyPromise.tryFailure( ex )
        }
      }
    }
  }

}
