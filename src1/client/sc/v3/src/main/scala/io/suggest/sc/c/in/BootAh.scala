package io.suggest.sc.c.in

import diode._
import diode.data.Pot
import io.suggest.async.IValueCompleter
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.maps.m.RcvrMarkersInit
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.Sc3Circuit
import io.suggest.sc.m._
import io.suggest.sc.m.boot._
import io.suggest.sc.m.in.MJsRouterS
import io.suggest.sc.m.search.MGeoTabData
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.Log
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.m.dia.first.{InitFirstRunWz, WzReadPermissions}
import io.suggest.sc.m.inx.MScSwitchCtx
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.CircuitUtil
import io.suggest.spa.DiodeUtil.Implicits._
import japgolly.univeq._

import scala.concurrent.{Future, Promise}
import scala.util.{Success, Try}

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
                 needBootGeoLocRO: () => Boolean,
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
    private def _startWithPot[X: IValueCompleter](serviceId   : MBootServiceId,
                                                  zoomR       : ModelR[MScRoot, X],
                                                  timeoutOkMs : Option[Int] = None): Future[BootStartCompleted] = {
      val ps = CircuitUtil.promiseSubscribe()
      for (timeoutOkMs1 <- timeoutOkMs)
        ps.withTimeout( timeoutOkMs1 )

      ps.zooming( circuit, zoomR )
        .transform { tryRes =>
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
        val bgFx = JsRouterInit().toEffectPure
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
        val bgFx = RcvrMarkersInit().toEffectPure
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
          val plat = circuit.platformRW.value

          if (plat.isCordova) {
            // cordova - Нужно повесится на platform-ready.
            val isReadyRO = circuit.platformRW.zoom(_.isReady)
            _startWithPot( serviceId, isReadyRO, timeoutOkMs = Some(7000) )
          } else {
            // browser - сразу вернуть ответ.
            Future.successful( this.finished() )
          }
        }
      }

    }


    /** Read current permissions st. */
    class ReadPermissionsSvc extends IBootService {

      override def serviceId = MBootServiceIds.ReadPermissions

      override def depends: List[MBootServiceId] = {
        val deps0 = super.depends
        val plat = circuit.platformRW.value
        if (plat.isCordova && !plat.isReady) {
          // Для неготовой cordova - надо дожидаться готовности:
          MBootServiceIds.Platform :: deps0
        } else {
          deps0
        }
      }

      override def startFx: Effect = {
        Effect.action {
          WzReadPermissions(
            onComplete = Some { Effect.action {
              this.finished()
            }}
          )
        }
      }

    }


    /** Сбор данных геолокации для первого запуска. */
    class GeoLocDataAccSvc extends IBootService {
      override def serviceId = MBootServiceIds.GeoLocDataAcc
      override def depends: List[MBootServiceId] =
        MBootServiceIds.ReadPermissions :: super.depends
      override def startFx: Effect = {
        if (circuit.wzFirstOuterRW.value.perms.hasLocationAccess) {
          val startHwFx = Effect.action {
            PeripheralStartStop(
              isStart = OptionUtil.SomeBool.someTrue,
              onDemand = false,
            )
          }

          // TODO Duplicates code inside PlatformAh()._geoLocControlFx(). Need to separate location timer starter from GPS controlling method in PlatformAh...
          val locationTimerFx = Effect.action {
            val sctx = MScSwitchCtx(
              indexQsArgs = MScIndexArgs(
                geoIntoRcvr = true,
                retUserLoc  = false,
              ),
              //demandLocTest = true,
            )
            GeoLocTimerStart( sctx )
          }

          startHwFx >> locationTimerFx >> Effect {
            _startWithPot(
              serviceId,
              zoomR = circuit.internalsInfoRW.zoom(_.geoLockTimer),
              timeoutOkMs = Some( 5000 ),
            )
          }

        } else {
          // No location permissions has been granted, skip this step:
          Effect.action( this.finished() )
        }
      }
    }

    class LanguageSvc extends IBootService {
      override def serviceId = MBootServiceIds.Language
      override def depends = MBootServiceIds.Platform :: MBootServiceIds.JsRouter :: Nil
      override def startFx: Effect = {
        LangInit.toEffectPure >> Effect {
          _startWithPot( serviceId, circuit.reactCtxRW.zoom(_.langSwitch), timeoutOkMs = Some(1500) )
        }
      }
    }


    /** Activate showcase index after initial geodata became available. */
    class InitShowcaseIndexSvc extends IBootService {
      override def serviceId = MBootServiceIds.InitShowcaseIndex
      override def depends = {
        var deps = MBootServiceIds.JsRouter :: MBootServiceIds.Language :: super.depends

        if ( needBootGeoLocRO() )
          deps ::= MBootServiceIds.GeoLocDataAcc

        deps
      }

      override def startFx: Effect = {
        // Activate showcase interface, start/await first index request/response.
        _reRouteFx( allowRouteTo = true ) >> Effect {
          // TODO Process first index request errors here.
          _startWithPot( serviceId, circuit.indexRW.zoom(_.resp) )
        }
      }
    }


    /** Show permissions wizard GUI. */
    class PermissionsGuiSvc extends IBootService {
      override def serviceId = MBootServiceIds.PermissionsGui
      override def depends = MBootServiceIds.InitShowcaseIndex :: super.depends
      override def startFx: Effect = {
        // Initialize permissions wizard, then wait until it finishes:
        val isNeedBootGeoLoc = needBootGeoLocRO()
        val initFx = Effect.action {
          InitFirstRunWz( isNeedBootGeoLoc )
        }
        val monitorPotFx = Effect {
          _startWithPot( serviceId, circuit.wzFirstOuterRW.zoom(_.view) )
        }
        initFx >> monitorPotFx
      }
    }


    /** Унифицированная сборка сервисов. */
    def make(id: MBootServiceId): IBootService = {
      id match {
        case MBootServiceIds.JsRouter               => new JsRouterSvc
        case MBootServiceIds.RcvrsMap               => new RcvrsMapSvc
        case MBootServiceIds.Platform               => new PlatformSvc
        case MBootServiceIds.ReadPermissions        => new ReadPermissionsSvc
        case MBootServiceIds.GeoLocDataAcc          => new GeoLocDataAccSvc
        case MBootServiceIds.PermissionsGui         => new PermissionsGuiSvc
        case MBootServiceIds.InitShowcaseIndex      => new InitShowcaseIndexSvc
        case MBootServiceIds.Language               => new LanguageSvc
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

        val prevTargetsHasSvc = (acc0.prevTargets contains svcId)
        if (
          // Эта служба уже [была] запущена, возможно уже работает?
          svcStartCompleted || prevTargetsHasSvc
        ) {
          // Эта цель инициализации уже была пройдена. Перейти к следующей цели.
          val acc2 = acc0.copy(
            restTargets         = restTargetsTl,
            stepsCounter        = acc0.stepsCounter + 1,
            // Всё ли запущено на данном уровне зависимостей?
            levelStartCompleted = acc0.levelStartCompleted  &&  svcDataOpt0.exists( _.isStartDoneSuccess )
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
              svcData2.isStartDoneSuccess
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


  private def _processStartState( v0: MScBoot, svcIds: List[MBootServiceId], afterFx: Option[Effect] = None): ActionResult[M] = {
    val startAcc2 = _processServiceStart(
      StartAcc(
        v0          = v0,
        restTargets = svcIds,
      )
    )

    val fxOpt = (startAcc2.fxAcc :: afterFx.toList :: Nil)
      .iterator
      .flatten
      .mergeEffects

    ah.updatedMaybeEffect( startAcc2.v0, fxOpt )
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал к запуску какого-либо процесса.
    case m: Boot =>
      val v0 = value

      // Нужно залить в состояние список запрошенных целей, которые ещё не запущены:
      // Нужно вычистить сервисы, чьё состояние уже отработано ранее:
      val svcIds2 = m.svcIds
        .filterNot { svcId =>
          (v0.services contains svcId) ||
          (v0.targets contains svcId)
        }

      if (svcIds2.isEmpty) {
        //logger.log( ErrorMsgs.ALREADY_DONE, msg = m )
        noChange
      } else {
        val v1 = MScBoot.targets.modify(_ ++ svcIds2)(v0)
        _processStartState( v1, svcIds2 )
      }


    // Сигнал завершения какого-то этапа.
    case m: BootStartCompleted =>
      // У какой-то запускаемой службы завершился start-эффект. Залить данные в состояние.
      val v0 = value
      v0.services
        .get( m.svcId )
        .fold {
          logger.warn( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
          noChange
        } { svcData0 =>
          var svcDataModF = MBootServiceState.started.set(
            Some(
              m.tryRes
                .map(_ => true)
            )
          )
          if (svcData0.after.nonEmpty)
            svcDataModF = svcDataModF andThen MBootServiceState.after.set( None )
          val svcData2 = svcDataModF(svcData0)

          // Залить полученную инфу в состояние.
          val v1 = v0.copy(
            services = v0.services + (m.svcId -> svcData2),
            targets  = if (v0.targets contains m.svcId)
              v0.targets - m.svcId
            else
              v0.targets
          )

          if (v1.targets.nonEmpty) {
            _processStartState( v1, v1.targets.toList, svcData0.after )
          } else {
            ah.updatedSilentMaybeEffect( v1, svcData0.after )
          }
        }


    // Подписка на окончание загрузки чего-либо.
    case m: BootAfter =>
      val v0 = value
      v0.services
        .get( m.svcId )
        .map { svcState0 =>
          // Есть инфа по состоянию загрузки указанного сервиса.
          if ( svcState0.started.exists { _.fold(_ => true, identity) } ) {
            // Загрузка уже завершена. Запустить эффект.
            effectOnly( m.fx )
          } else {
            // Загрузка пока не завершена. Надо закинуть в состояние инфу по эффекту.
            val svcState2 = MBootServiceState.after.modify { fxOpt0 =>
              val fx2 = fxOpt0.fold(m.fx)(_ + m.fx)
              Some( fx2 )
            }(svcState0)
            val v2 = MScBoot.services.modify(_ + (m.svcId -> svcState2))(v0)
            updatedSilent(v2)
          }
        }
        .getOrElse {
          // Состояние сервис не найдено в карте сервисов.
          m.ifMissing.fold {
            logger.log( ErrorMsgs.NODE_NOT_FOUND, msg = m )
            noChange
          }( effectOnly )
          // TODO Надо как-то логгировать по-нормальному ошибки внутри эффекта, иначе трудно что-либо понять без ковыряния в отладчике.
        }

  }

  private def _reRouteFx(allowRouteTo: Boolean): Effect = {
    Effect.action {
      (for {
        currRoute <- OptionUtil.maybeOpt( allowRouteTo ) {
          circuit.internalsRW
            .value
            .info.currRoute
        }
      } yield {
        RouteTo( currRoute )
      })
        .getOrElse {
          ResetUrlRoute(
            force = true,
          )
        }
    }
  }

}
