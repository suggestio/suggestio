package io.suggest.sc.sjs.c.scfsm.geo

import io.suggest.sc.sjs.c.gloc.GeoLocFsm
import io.suggest.sc.sjs.c.scfsm.node.Index
import io.suggest.sc.sjs.m.mgeo._
import io.suggest.sc.sjs.util.router.srv.SrvRouter
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Promise

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.04.16 10:32
  * Description: Инициализатор второго поколения для гео-выдачи второго поколения.
  *
  * Первый инициализатор был реализован через цепочку состояний, но уже на раннем этапе состояния
  * стали переусложненными, и их стало слишком много для одного, хоть и сложного, действия.
  *
  * Было решено унифицировать и использовать для этого всего цепочки из Future.
  * В итоге получилось гибридное состояние: таймауты и асинхронные результаты нередко приходят как FSM-сообщения,
  * но другие вещи разруливаются на уровне future'ов.
  *
  * Цель в целом достигнута: *только* одно состояние гео-инициализации начальной выдачи с легко-читаемым кодом.
  * Из костылей: только Promise внутри private var состояния.
  */
trait GeoScInit extends Index { me =>

  /** Таймаут геолокации при запуске. */
  private def INIT_GEO_TIMEOUT_MS = 5500

  /** Трейт состояния инициализации в сторону геовыдачи. */
  trait GeoScInitStateT
    extends FsmEmptyReceiverState
      with GetIndexUtil
      with ProcessIndexReceivedUtil
  {
    /** var-костыль чтобы не запихивать редкоиспользуемый mutable promise в immutable-состояние.
      * Вообще, необходимость таких переменных состояния намекает на проблемы в архитектуре FSM. */
    private var _gpsGeoLocP: Promise[MGeoLoc] = _

    /** Запуск инициализации в сторону геовыдачи. */
    def _geoScInit(): Unit = {

      // Сразу запустить получение геолокации из браузера.
      GeoLocFsm ! Subscribe(
        receiver    = me,
        notifyZero  = true,
        data        = SubscriberData(
          minWatch    = GlWatchTypes.Gps,
          withErrors  = true
        )
      )

      // Запустить получение js-роутера. Это просто нужно, в т.ч. для получения sc-индекса.
      val jsRouterFut = SrvRouter.getRouter()

      // TODO Opt index-preload тут на случай проблем с геолокацией
      // И стоит не забыть про if ( mNodeIndex.geoAccurEnought.contains(true) ) ?

      // Результат геолокации придёт сюда из receiver'а. Туда promise будет передан через stateData, что является этой проблемой архитектуры.
      _gpsGeoLocP = Promise[MGeoLoc]()

      // Запустить таймер максимального ожидания геолокации.
      val geoLocTimerId = DomQuick.setTimeout(INIT_GEO_TIMEOUT_MS) { () =>
        _sendEventSync( GeoTimeout )
      }

      _stateData = {
        val sd0 = _stateData
        sd0.copy(
          geo = sd0.geo.copy(
            timer = Some(geoLocTimerId)
          )
        )
      }

      val gpsGeoLocFut = _gpsGeoLocP.future
        .recover { case ex: Throwable =>
          if (ex != null && !ex.isInstanceOf[NoSuchElementException])
            warn( ErrorMsgs.GEO_LOC_FAILED, ex )
          null
        }

      // Когда будет выполнена геолокация и js-роутер, надо затребовать индекс грядущей выдачи.
      for {
        _     <- jsRouterFut
        _     <- gpsGeoLocFut
        // Данные геолокации уже выставлены в stateData в ресиверах состояния, можно вызывать запрос индекса.
        inx   <- _getIndex()
      } {
        // Накатить индекс на выдачу
        _nodeIndexReceived(inx)
      }
    }


    override def afterBecome(): Unit = {
      super.afterBecome()
      // Запустить гео-инициализацию выдачи
      _geoScInit()
    }

    override def receiverPart: Receive = super.receiverPart.orElse {
      // Сигнал об успешных данных геолокации.
      case glLoc: GlLocation =>
        _handleGlLocation(glLoc)

      // Сигнал таймаута ожидания точной геолокации
      case GeoTimeout if _stateData.geo.timer.nonEmpty =>
        _handleGpsGeoTimeout()

      // Сигнал об отсутствии какой-либо геолокации
      case GlUnknown =>
        _handleGeoLocUnknown()

      // Сигнал о невозможности точной геолокации на текущем юзер-агенте.
      case glErr: GlError =>
        _handleGlError(glErr)
    }


    /** Реакция на успешное получение данных геолокации. */
    def _handleGlLocation(glLoc: GlLocation): Unit = {
      val sd0 = _stateData

      // Отменить таймер таймаута геолоакции.
      for (timerId <- sd0.geo.timer) {
        DomQuick.clearTimeout(timerId)
        // Отменить подписку ScFsm на события геолокации. Если таймер уже отменен, то значит и подписка уже была отменена.
        _glUnSubscribe()
      }

      // Сохранить в состояние.
      _stateData = sd0.copy(
        geo = sd0.geo.copy(
          lastGeoLoc  = Some(glLoc.data),
          timer       = None
        )
      )

      // Выполнить promise поиска геолокации. Скорее всего это будем синхронная операция, поэтому в конце метода.
      _gpsGeoLocP.trySuccess( glLoc.data )
    }


    /** Реакция на таймаут точной геолокации. */
    def _handleGpsGeoTimeout(): Unit = {
      // Запросить у GeoLocFsm любую доступную геолокацию.
      GeoLocFsm ! GetAnyGl(me)

      // Отказаться от событий геолоакции.
      _glUnSubscribe()

      // Сохранить в состояние исполненный таймер.
      val sd0 = _stateData
      _stateData = sd0.copy(
        geo = sd0.geo.copy(
          timer = None
        )
      )
    }


    /** Реакция на сигнал об отсутствии геолокации. */
    def _handleGeoLocUnknown(): Unit = {
      // На всякий случай. По идее unsubscribe был уже выполнен в _handleGpsGeoTimeout, т.к. GlUnknown не приходит внутри subscribe.
      _glUnSubscribe()

      // Обновить состояние ScFsm.
      val sd0 = _stateData
      _stateData = sd0.copy(
        geo = sd0.geo.copy(
          // lastGeoLoc пропускаем, т.к. там или None или более-менее интересная геолокация.
          // по идее, таймера быть уже не должно. В любом случае он не интересен.
          timer   = None
        )
      )

      // Исполнить фьючерс геолокации
      _gpsGeoLocP.tryFailure( new NoSuchElementException() )
    }


    /** Реакция на невозможность геолокации. */
    def _handleGlError(glErr: GlError): Unit = {
      for (timerId <- _stateData.geo.timer) {
        DomQuick.clearTimeout(timerId)
      }
      _handleGeoLocUnknown()
    }


    /** Уведомления геолокации пока больше не интересны. */
    private def _glUnSubscribe(): Unit = {
      GeoLocFsm ! UnSubscribe(me)
    }

  }

}
