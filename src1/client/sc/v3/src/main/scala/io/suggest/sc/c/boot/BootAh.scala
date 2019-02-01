package io.suggest.sc.c.boot

import diode._
import diode.data.Pot
import io.suggest.ble.beaconer.m.BtOnOff
import io.suggest.common.html.HtmlConstants
import io.suggest.maps.m.RcvrMarkersInit
import io.suggest.msg.WarnMsgs
import io.suggest.sc.Sc3Circuit
import io.suggest.sc.m.boot._
import io.suggest.sc.m.dia.InitFirstRunWz
import io.suggest.sc.m.{GeoLocOnOff, JsRouterInit, MScRoot}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.dom.DomQuick
import io.suggest.spa.DAction
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
          // Отменить таймаут закрытия фьючерса:
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

    private def _oneShotStartFx(serviceId: MBootServiceId)(actions: => List[DAction]): Effect = {
      Effect {
        actions
          .iterator
          .++( Iterator.single( BootStartCompleted(serviceId, Success(None)) ) )
          .map(_.toEffectPure)
          .mergeEffects
          .get
          .toFuture
          .asInstanceOf[Future[DAction]]
      }
    }

    /** Описание процедуры js-роутера. */
    class JsRouterSvc extends IBootService {

      override def serviceId = MBootServiceIds.JsRouter

      override def startFx: Effect = {
        val fx = Effect( _startWithPot( serviceId, circuit.jsRouterRW ) )
        circuit.dispatch( JsRouterInit )
        fx
      }
      // TODO Sc3Main содержит свой запуск jsRouter-инициализации, который ещё активирует RmeLogging.

    }


    /** Спека инициализации карты ресиверов. */
    class RcvrsMapSvc extends IBootService {

      override def serviceId = MBootServiceIds.RcvrsMap

      override def depends =
        MBootServiceIds.JsRouter :: Nil

      override def startFx: Effect = {
        val fx = Effect( _startWithPot( serviceId, circuit.geoTabDataRW.zoom(_.rcvrsCache) ) )
        circuit.dispatch( RcvrMarkersInit )
        fx
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
            _startWithPot( serviceId, circuit.platformRW.zoom(_.isReady), timeoutOkMs = Some(7000) )
          } else {
            // browser - сразу вернуть ответ.
            Future.successful( this.finished() )
          }
        }
      }

    }

    /** Спека для инициализации геолокации. */
    class GpsSvc extends IBootService {
      override def serviceId = MBootServiceIds.Gps
      override def depends = MBootServiceIds.Platform :: Nil
      override def startFx: Effect = {
        _oneShotStartFx(serviceId) {
          val action = GeoLocOnOff(enabled = true, isHard = false)
          action :: Nil
        }
      }
    }

    /** Спека для инициализации слушанья маячков Bluetooth. */
    class BlueToothBeaconingSvc extends IBootService {
      override def serviceId = MBootServiceIds.BlueToothBeaconing
      override def depends = MBootServiceIds.Platform :: Nil
      override def startFx: Effect = {
        _oneShotStartFx(serviceId) {
          // В хвост списка - сразу уведомление о завершении запуска.
          if (circuit.platformRW.value.hasBle)
            // Отправить команду
            BtOnOff( isEnabled = true ) :: Nil
          else
            Nil
        }
      }
    }

    /** Спека инициализации мастера первого запуска. */
    class FirstRunWizardSvc extends IBootService {
      override def serviceId = MBootServiceIds.FirstRunWizard
      override def startFx: Effect = {
        _oneShotStartFx(serviceId) {
          val action = InitFirstRunWz(true)
          action :: Nil
        }
      }
      // depends не прописываем, т.к. для начального экрана ничего не нужно, а для остальных - идёт ручной запуск служб.
    }

    /** Сбор данных геолокации для первого запуска. */
    class GeoLocDataAccSvc extends IBootService {
      override def serviceId = MBootServiceIds.GeoLocDataAcc
      override def startFx: Effect = {
        // Довольно архаичный и неотъемлемый элемент запуска:
        // задержка запуска для начального накопления данных геолокации.
        //Effect {
        //  ???
        //}
        ???
      }
    }

    /** Унифицированная сборка сервисов. */
    def make(id: MBootServiceId): IBootService = {
      id match {
        case MBootServiceIds.JsRouter               => new JsRouterSvc
        case MBootServiceIds.RcvrsMap               => new RcvrsMapSvc
        case MBootServiceIds.Platform               => new PlatformSvc
        case MBootServiceIds.Gps                    => new GpsSvc
        case MBootServiceIds.BlueToothBeaconing     => new BlueToothBeaconingSvc
        case MBootServiceIds.GeoLocDataAcc          => new GeoLocDataAccSvc
      }
    }

  }

  /** Получение целей-зависимостей для одной boot-цели.
    *
    * @param bootTg Цель загрузки.
    * @return Список цели для инициализации.
    */
  private def depends(bootTg: MBootServiceId): List[MBootServiceId] = {
    bootTg match {

      // Для инициализации новой выдачи нужен доступ к собранным данным местоположения.
      // Но если узел или иные данные уже заданы, то сбор геоданных не требуется.
      case MBootServiceIds.IndexGrid =>
        val mroot = circuit.rootRW.value
        val istate = mroot.index.state
        if (istate.rcvrId.isEmpty && istate.inxGeoPoint.isEmpty) {
          // Требуется полная инициализация.
          MBootServiceIds.FirstRunWizard ::
          MBootServiceIds.GeoLocDataAcc ::
          Nil
        } else {
          // Уже есть какие-то координаты или id начального узла - идём по упрощёнке:
          Nil
        }

      // Для доступа к оборудования нужно platform-ready.
      case MBootServiceIds.BlueToothBeaconing | MBootServiceIds.Gps =>
        MBootServiceIds.Platform ::
        Nil

      // Для инициализации карты ресиверов, нужен инициализированный js-роутер.
      case MBootServiceIds.RcvrsMap =>
        MBootServiceIds.JsRouter ::
        Nil

      // Нначальное накопление капитала данных геолокации
      case MBootServiceIds.GeoLocDataAcc =>
        MBootServiceIds.Gps ::
        MBootServiceIds.BlueToothBeaconing ::
        Nil

      // У остальных целей инициализации - нет зависимостей.
      case _ =>
        Nil

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
            svcData0.withStarted(
              started = Some( Success(false) )
            )
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
      //println(m)
      val v0 = value

      // Нужно залить в состояние список запрошенных целей, которые ещё не запущены:
      val v1 = v0.withTargets(
        v0.targets ++ m.svcIds
      )
      _processStartState( v1, m.svcIds )


    // Сигнал завершения какого-то этапа.
    case m: BootStartCompleted =>
      //println(m)
      // У какой-то запускаемой службы завершился start-эффект. Залить данные в состояние.
      val v0 = value
      v0.services
        .get( m.svcId )
        .fold {
          LOG.warn( WarnMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
          noChange
        } { svcData0 =>
          val svcData2 = svcData0.withStarted(
            Some(
              m.tryRes
               .map(_ => true)
            )
          )
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
            updatedSilent( v1 )
          }
        }

  }

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
